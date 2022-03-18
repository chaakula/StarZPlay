/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.kpodata.service.impl;

import static java.util.Objects.isNull;

import java.io.IOException;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.apache.commons.collections4.CollectionUtils;
import org.bson.Document;
import org.json.JSONException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kpodata.config.MongoMultiTenant;
import com.kpodata.dto.HolidayDto;
import com.kpodata.exception.KpoDataException;
import com.kpodata.model.DataRequest;
import com.kpodata.model.FieldAuth;
import com.kpodata.model.ResponseMessage;
import com.kpodata.mongo.documents.AuditLogs;
import com.kpodata.mongo.documents.Employee;
import com.kpodata.mongo.documents.EmployeeLeaveBalance;
import com.kpodata.mongo.documents.FormAutoIncrements;
import com.kpodata.mongo.documents.FormField;
import com.kpodata.mongo.documents.FormInfo;
import com.kpodata.mongo.documents.Holidays;
import com.kpodata.mongo.documents.Leave;
import com.kpodata.mongo.documents.LeaveTypes;
import com.kpodata.mongo.documents.Role;
import com.kpodata.repository.AuditLogRepo;
import com.kpodata.repository.CustomAuditRepo;
import com.kpodata.repository.EmployeeLeaveBalanceRepo;
import com.kpodata.repository.EmployeeRepo;
import com.kpodata.repository.FormAutoIncrementRepo;
import com.kpodata.repository.FormRepo;
import com.kpodata.repository.HolidayRepo;
import com.kpodata.repository.LeaveRepo;
import com.kpodata.repository.LeaveTypeRepo;
import com.kpodata.service.AWSService;
import com.kpodata.util.KpoCacheInstance;
import com.kpodata.util.MongoDBDistint;
import com.kpodata.util.SubscriptionEnum;
import com.kpodata.util.SystemFields;
import com.kpodata.util.UserRoleValidation;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.result.UpdateResult;

import lombok.extern.slf4j.Slf4j;

/**
 * @author pasrinivas
 */
@Service
@Slf4j
public class DataOperationServiceImpl {

	@Autowired
	private MongoMultiTenant mongoMultiTenant;

	@Autowired
	private MongoTemplate mongoTemplate;
	@Autowired
	private UserRoleValidation urv;

	@Value("${host.name}")
	private List<String> host;
	@Autowired
	RestTemplate restTemplate;
	@Autowired
	PasswordEncoder encoder;
	@Autowired
	CustomAuditRepo customAuditRepo;
	@Autowired
	HolidayRepo holidayRepo;
	@Autowired
	EmployeeRepo employeeRepo;
	@Autowired
	LeaveRepo leaveRepo;
	@Autowired
	AuditLogRepo auditLogRepo;
	@Autowired
	MongoDBDistint mongoDBDistint;
	@Autowired
	private AWSService awss;
	@Autowired
	private FormRepo formRepo;

	@Value("${kpodata.aws.bucketname}")
	private String bucketName;
	@Value("${kafka.topic.workflow}")
	private String workFlowTopic;
//	@Value("${auth.enable}")
//	private Boolean isAuthEnable;

	@Autowired
	FormAutoIncrementRepo autoIncrementRepo;
	@Autowired
	LeaveTypeRepo leaveTypeRepo;
	@Autowired
	KpoCacheInstance cacheInstance;
	@Autowired
	LMSServiceImpl leaveService;
	@Autowired
	WorkFlowExecutorService executorService;
	@Autowired
	RedisTemplate<String, Object> redisTempalte;
	@Autowired
	KafkaTemplate kafkaTemplate;
	@Autowired
	LMSServiceImpl lmssi;
	
	@Autowired
	private EmployeeLeaveBalanceRepo employeeLeaveBalRepo;

	private boolean isCollectionExist(String collectionName) {
		boolean collectionExists = mongoMultiTenant.getDb().listCollectionNames().into(new HashSet<>())
				.contains(collectionName.toLowerCase());
		return collectionExists;
	}

	public String insertData(DataRequest dataRequest, String isSubscriptionExpired)
			throws JSONException, JsonProcessingException, IOException, KpoDataException {
		log.info("Document Update method called");
		mongoDBDistint.packageIsExpired(isSubscriptionExpired);
		// if (isAuthEnable) {
			if (urv.isUserSuperAdmin(dataRequest.getUserID())) {
				log.info("Role is SuperAdmin");
			} else {
				if (urv.validateUserCreatePermissions(dataRequest) == null) {
					ResponseMessage responseMessage = new ResponseMessage();
					HashMap<String, Object> data = new HashMap<>();
					responseMessage.setData(data);
					responseMessage.setHttpStatus(HttpStatus.FORBIDDEN);
					responseMessage.setCode(403);
					responseMessage.setMsg("User not allowed for this operation");
					throw new KpoDataException("FORBIDDEN", responseMessage);
				}
				if (CollectionUtils.isNotEmpty(urv.validateFieldCreatePermissions(dataRequest))) {
					Set<String> restrictFields = urv.validateFieldCreatePermissions(dataRequest);
					ResponseMessage responseMessage = new ResponseMessage();
					HashMap<String, Object> data = new HashMap<>();
					data.put("fields", restrictFields);
					responseMessage.setData(data);
					responseMessage.setHttpStatus(HttpStatus.FORBIDDEN);
					responseMessage.setCode(403);
					responseMessage.setMsg("User don't have write permission on following fields");
					throw new KpoDataException("FORBIDDEN", responseMessage);
				}
			}
		// }

		Double noOfDays = null;
		String formName = dataRequest.getFormName().toLowerCase();
		if (formName.equalsIgnoreCase("leave")) {
			noOfDays = validateLeaveRequest(dataRequest); 
		} else if (formName.toLowerCase().equalsIgnoreCase("employee")) {
			mongoDBDistint.reachLimitForRecord(dataRequest.getTenantID(), "employee", SubscriptionEnum.USER);
		}
		String tenant = dataRequest.getTenantID();
		String formKey = tenant + "@" + formName;
		populateFormId(dataRequest, formKey);
		boolean isexist = isCollectionExist(formName);
		Map<String, Object> requestData = dataRequest.getData();
		if (isexist) {
			Long previousId = 0L;
			FormAutoIncrements autoIncrements = autoIncrementRepo.findByFormName(formName);
			if (autoIncrements != null) {
				previousId = autoIncrements.getForm_auto_id();

			}
			Long currentid = previousId + 1;
			autoIncrements.setForm_auto_id(currentid);
			// ObjectId id = new ObjectId();
			String id = UUID.randomUUID().toString().replaceAll("-", "");
			Instant instant = Instant.now();
			long timeStampMillis = instant.toEpochMilli();
			requestData.put("system_active", true);
			requestData.put("tenantID", dataRequest.getTenantID());
			requestData.put("formName", dataRequest.getFormName());
			requestData.put("formID", dataRequest.getFormID());
			requestData.put("createdBy", dataRequest.getUserID());
			requestData.put("createdDate", timeStampMillis);
			requestData.put("system_auto_id", currentid);
			requestData.put("_id", id);
			for (Map.Entry<String, Object> entry : requestData.entrySet()) {
				log.debug(entry.getKey() + ":" + entry.getValue());
//                String lookUpField = formKey + "@" + entry.getKey();
//                if (isPwdField(lookUpField)) {
//                    String encPwd = encoder.encode(entry.getValue().toString());
//                    entry.setValue(encPwd);
//
//                }
				if (isMapOrNot(entry.getValue())) {
					log.info("Tabular Data");
					List<Map<String, Object>> tablurList = (List<Map<String, Object>>) entry.getValue();
					for (Map<String, Object> tablurObj : tablurList) {
						String tabID = UUID.randomUUID().toString().replaceAll("-", "");
						tablurObj.put("id", tabID);
						tablurObj.put("system_active", true);
						tablurObj.put("createdBy", dataRequest.getUserID());
						tablurObj.put("createdDate", timeStampMillis);
						tablurObj.put("updatedBy", dataRequest.getUserID());
						tablurObj.put("updatedDate", timeStampMillis);
					}
				} else {
					if (mongoDBDistint.isUniqueField(tenant, formName, entry.getKey())) {
						log.info("unique field:{}", entry.getKey());
						mongoDBDistint.uniqueValue(formName, entry.getKey(), entry.getValue(), 0, null);
					} else if (mongoDBDistint.isAuditEnable(tenant, formName, entry.getKey())) {
						log.info("audit field:{}", entry.getKey());
						AuditLogs auditLogs = new AuditLogs();
						auditLogs.setFormName(formName);
						auditLogs.setFieldName(entry.getKey());
						auditLogs.setDocID(id);
						auditLogs.setInsertedData(requestData);
						auditLogs.setRecordOperation("INSERT");
						auditLogs.setCreatedBy(dataRequest.getUserID());
						auditLogs.setCreatedDate(timeStampMillis);
						auditLogs.setUpdatedDate(timeStampMillis);
						auditRecord(auditLogs); // auditing record
					}

				}
			}

			Map<String, Object> insertedDoc = mongoTemplate.insert(requestData, formName);
			autoIncrementRepo.save(autoIncrements);
			dataRequest.setOperation("create");
			dataRequest.setData(insertedDoc);
			dataRequest.setSystem_auto_id(currentid);
			

			// executorService.executeWorkflow("create", dataRequest);
			ObjectMapper mapper = new ObjectMapper();
			String newRecord = mapper.writeValueAsString(dataRequest);
			sendToKafka(newRecord);
			// kafkaTemplate.send(workFlowTopic, newRecord);
			if (formName.equalsIgnoreCase("employee")) {
				Map<String, Object> upData = getByID(id, formName);
				cacheInstance.setEmployeeData(upData, formName, tenant);
			}
			if (formName.equalsIgnoreCase("leave")) {
				EmployeeLeaveBalance leaveBalance = employeeLeaveBalRepo.findByEmployeeIDAndLeaveTypeID( String.valueOf(dataRequest.getData().get("Employee")), String.valueOf(dataRequest.getData().get("Leave_Type")));
				leaveBalance.setNoOfDays(leaveBalance.getNoOfDays() - noOfDays);
				Double appliedLeaves = !isNull(leaveBalance.getAppliedDays()) ? leaveBalance.getAppliedDays() : Double.valueOf(0.0);
				leaveBalance.setAppliedDays(appliedLeaves+noOfDays);
				employeeLeaveBalRepo.save(leaveBalance);
			}
			return id;

		} else {
			log.error("Form name does not exist :{}", dataRequest.getFormName());
			ResponseMessage responseMessage = new ResponseMessage();
			HashMap<String, Object> data = new HashMap<>();
			data.put("formName", dataRequest.getFormName());
			responseMessage.setData(data);
			responseMessage.setHttpStatus(HttpStatus.BAD_REQUEST);
			responseMessage.setMsg("Form name does not exist");
			responseMessage.setCode(400);
			throw new KpoDataException("Form name does not exist", responseMessage);
		}

	}

	private void populateFormId(DataRequest dataRequest, String formKey) {
		if (redisTempalte.opsForHash().hasKey("formInfo", formKey)) {
			FormInfo formInfo = (FormInfo) redisTempalte.opsForHash().get("formInfo", formKey);
			dataRequest.setFormID(formInfo.getFormID());
		} else if (dataRequest.getFormID() == null) {
			List<FormInfo> forms = formRepo.findAll();
			forms.forEach(form -> {
				if(form.getName().replaceAll(" ","_").equalsIgnoreCase(dataRequest.getFormName())) {
					dataRequest.setFormID(form.getFormID());
				}
			});
		}
	}

	public Map<String, Object> updateData(DataRequest dataRequest, String isSubscriptionExpired) throws JSONException, KpoDataException, IOException {
		log.info("Document Update method called");

		mongoDBDistint.packageIsExpired(isSubscriptionExpired);
		String tenant = dataRequest.getTenantID();
		String formName = dataRequest.getFormName().toLowerCase();

		String formKey = tenant + "@" + formName;
		populateFormId(dataRequest, formKey);
		// if (isAuthEnable) {
			if (urv.isUserSuperAdmin(dataRequest.getUserID())) {
				log.info("Role is SuperAdmin");
			} else {
				if (urv.validateUserUpdatePermissions(dataRequest) == null) {
					ResponseMessage responseMessage = new ResponseMessage();
					HashMap<String, Object> data = new HashMap<>();
					responseMessage.setData(data);
					responseMessage.setHttpStatus(HttpStatus.FORBIDDEN);
					responseMessage.setCode(403);
					responseMessage.setMsg("User not allowed for this operation");
					throw new KpoDataException("FORBIDDEN", responseMessage);
				}
				if (CollectionUtils.isNotEmpty(urv.validateFieldUpdatePermissions(dataRequest))) {
					Set<String> restrictFields = urv.validateFieldUpdatePermissions(dataRequest);
					ResponseMessage responseMessage = new ResponseMessage();
					HashMap<String, Object> data = new HashMap<>();
					data.put("fields", restrictFields);
					responseMessage.setData(data);
					responseMessage.setHttpStatus(HttpStatus.FORBIDDEN);
					responseMessage.setCode(403);
					responseMessage.setMsg("User don't have update permission on following fields");
					throw new KpoDataException("FORBIDDEN", responseMessage);
				}
			}
		// }
		
		Double noOfDays = null;		
		Leave leaveRequest = null;
		if (formName.equalsIgnoreCase("leave")) {
			noOfDays = validateLeaveRequest(dataRequest); 
			leaveRequest = leaveRepo.findByLeaveID(String.valueOf(dataRequest.getData().get("_id")));
		}		
		
		boolean isexist = isCollectionExist(dataRequest.getFormName().toLowerCase());
		Map<String, Object> data = dataRequest.getData();
		Map<String, Object> updatedRecord = new HashMap<>();
		String newTabId = null;
		String oldTabId = null;
		String sessionName = null;
		String tabularSessionName = null;
		Instant instant = Instant.now();
		long timeStampMillis = instant.toEpochMilli();
		if (isexist) {
			if (data.containsKey("_id")) {
				// ObjectId docID = new ObjectId(data.get("_id").toString());
				String docID = data.get("_id").toString();
				MongoCollection<Document> collection = mongoTemplate.getDb().getCollection(formName);
				BasicDBObject query = new BasicDBObject();
				BasicDBObject update = new BasicDBObject();
				BasicDBObject insert = new BasicDBObject();
				BasicDBObject command = new BasicDBObject();
				query.put("_id", docID);
				for (Map.Entry<String, Object> entry : data.entrySet()) {
					log.debug(entry.getKey() + ":" + entry.getValue());
					sessionName = entry.getKey();
					boolean isNumeric = true;
					try {
						// Linear section, File upload with multiple files, will send the array of
						// objects
						// isMapOrNot() method is considering such scenario as Tabular section and hence
						// it is failing to update the content.
						// If a key is NOT A NUMBER i am assuming the field is linear section field.
						// 'isNumeric' is used for the same.
						Double num = Double.parseDouble(sessionName);
					} catch (NumberFormatException e) {
						isNumeric = false;
					}
					if (isNumeric && isMapOrNot(entry.getValue())) {
						log.debug("Tabular Data" + entry.getValue());
						List<Map<String, Object>> tablurList = (List<Map<String, Object>>) entry.getValue();
						for (Map<String, Object> tablurObj : tablurList) {
							if (tablurObj.containsKey("id")) {
//                                ObjectId tabularID = new ObjectId(tablurObj.get("_id").toString());
								oldTabId = tablurObj.get("id").toString();
								query.put(entry.getKey() + ".id", oldTabId);
								for (Map.Entry<String, Object> tablurInnerObj : tablurObj.entrySet()) {
									log.debug(tablurInnerObj.getKey() + ":" + tablurInnerObj.getValue());
									if (!SystemFields.systemFields().contains(tablurInnerObj.getKey())) {
										update.put(entry.getKey() + ".$." + tablurInnerObj.getKey(),
												tablurInnerObj.getValue());
										update.put(entry.getKey() + ".$.updatedBy", dataRequest.getUserID());
										update.put(entry.getKey() + ".$.updatedDate", timeStampMillis);
									}
								}
								command.put("$set", update);
								log.debug("Updated tabular field  in: {} ",
										entry.getKey() + " with field value is : {}", update);
							} else {
								newTabId = UUID.randomUUID().toString().replaceAll("-", "");
								insert.put("id", newTabId);
								for (Map.Entry<String, Object> tablurInnerObj : tablurObj.entrySet()) {
									log.debug(tablurInnerObj.getKey() + ":" + tablurInnerObj.getValue());
									if (!SystemFields.systemFields().contains(tablurInnerObj.getKey())) {
										insert.put(tablurInnerObj.getKey(), tablurInnerObj.getValue());
									}
								}
								insert.put("system_active", true);
								insert.put("createdBy", dataRequest.getUserID());
								insert.put("createdDate", timeStampMillis);
								insert.put("updatedBy", dataRequest.getUserID());
								insert.put("updatedDate", timeStampMillis);
								insert.put("formName", dataRequest.getFormName());
								insert.put("formID", dataRequest.getFormID());
								DBObject insertNew = new BasicDBObject(entry.getKey(), insert);
								command.put("$push", insertNew);
								log.debug("New tabular field added  in: {} ",
										entry.getKey() + " with field value is : {}", insert);
							}
						}
					} else {
						if (!SystemFields.systemFields().contains(entry.getKey())) {
							if (mongoDBDistint.isUniqueField(tenant, formName, entry.getKey())) {
								log.info("unique field:{}", entry.getKey());
								mongoDBDistint.uniqueValue(formName, entry.getKey(), entry.getValue(), 1, docID);
							}
							update.put(entry.getKey(), entry.getValue());
							log.debug("Linear session fields Data" + entry.getValue());
							update.put("updatedBy", dataRequest.getUserID());
							update.put("updatedDate", timeStampMillis);
							update.put("formName", dataRequest.getFormName());
							update.put("formID", dataRequest.getFormID());
							command.put("$set", update);
							log.debug("New Linear field added in: {} ", entry.getKey() + " with field value is : {}",
									entry.getValue());
							if (mongoDBDistint.isAuditEnable(tenant, formName, entry.getKey())) {
								AuditLogs auditLogs = new AuditLogs();

								if (newTabId != null) {
									auditLogs.setRowOperation("INSERT");
									updatedRecord.put("rowID", newTabId);

								} else if (oldTabId != null) {
									auditLogs.setRowOperation("UPDATE");
									updatedRecord.put("rowID", oldTabId);
								}
								updatedRecord.put("session", entry.getKey());
								updatedRecord.put("docID", docID);
								auditLogs.setFormName(formName);
								auditLogs.setDocID(docID);
								auditLogs.setFieldName(entry.getKey());
								auditLogs.setFieldValue(entry.getValue());
								auditLogs.setRequestedData(data);
								auditLogs.setUpdatedData(updatedRecord);
								auditLogs.setRecordOperation("UPDATE");
								auditLogs.setUpdatedBy(dataRequest.getUserID());
								auditLogs.setCreatedDate(timeStampMillis);
								auditLogs.setUpdatedDate(timeStampMillis);
								auditRecord(auditLogs);
								log.debug("Document audit successfully");
							}

						}

					}
				}
				log.debug("query :{}", query);
				log.debug("Command :{}", command);
				UpdateResult result = collection.updateOne(query, command);
				log.debug("result:{}", result);
				if (result.isModifiedCountAvailable()) {
					ObjectMapper mapper = new ObjectMapper();
					// executorService.executeWorkflow("update", dataRequest);
					dataRequest.setOperation("edit");
//                    String formKey = tenant + "@" + formName;
//                    if (redisTempalte.opsForHash().hasKey("formInfo", formKey)) {
//                        FormInfo formInfo = (FormInfo) redisTempalte.opsForHash().get("formInfo", formKey);
//                        dataRequest.setFormID(formInfo.getFormID());
//                    }
					Map<String, Object> upData = getByID(docID, formName);
					if (formName.equalsIgnoreCase("employee")) {

						// Employee employee =
						// employeeRepo.findByEmailAndActive(upData.get("email").toString(), true);
						cacheInstance.setEmployeeData(upData, formName, tenant);
					}
					
					
					if (formName.equalsIgnoreCase("leave")) {				
						if(leaveRequest != null) {
							Double oldNoOfDays = leaveRequest.getNoOfDays();
							if(!oldNoOfDays.equals(noOfDays)) {
								EmployeeLeaveBalance leaveBalance = employeeLeaveBalRepo.findByEmployeeIDAndLeaveTypeID(String.valueOf(dataRequest.getData().get("Employee")), String.valueOf(dataRequest.getData().get("Leave_Type")));
								Double diff = oldNoOfDays - noOfDays;
								leaveBalance.setNoOfDays(leaveBalance.getNoOfDays()+diff);
								leaveBalance.setAppliedDays(noOfDays);
								employeeLeaveBalRepo.save(leaveBalance);
							}
						}						
					}
					
					dataRequest.setSystem_auto_id(Long.parseLong(upData.get("system_auto_id").toString()));

					String kafkaRecord = mapper.writeValueAsString(dataRequest);
					sendToKafka(kafkaRecord);

					return getByID(docID, formName);

				} else {
					log.debug("nothing to updated ");
					return getByID(docID, formName);
				}
			} else {
				log.error("_id field is required for update");
				ResponseMessage responseMessage = new ResponseMessage();
				HashMap<String, Object> errorObj = new HashMap<>();
				errorObj.put("_id", "_id");
				responseMessage.setData(errorObj);
				responseMessage.setHttpStatus(HttpStatus.BAD_REQUEST);
				responseMessage.setMsg("_id field is required for update");
				responseMessage.setCode(400);
				throw new KpoDataException("_id field is required for update", responseMessage);
			}

		} else {
			log.error("Form Does not exist : {}", dataRequest.getFormName());
			ResponseMessage responseMessage = new ResponseMessage();
			HashMap<String, Object> errorObj = new HashMap<>();
			errorObj.put("formName", dataRequest.getFormName());
			responseMessage.setData(errorObj);
			responseMessage.setHttpStatus(HttpStatus.BAD_REQUEST);
			responseMessage.setMsg("Form does not exist");
			responseMessage.setCode(400);
			throw new KpoDataException("Form does not exist", responseMessage);
		}
	}

	@Async
	public void auditRecord(AuditLogs auditLogs) {
		log.info("async tasks are in progress");
		auditLogRepo.save(auditLogs);
	}

	@Async
	@SuppressWarnings("unchecked")
	public void sendToKafka(String msg) {
		log.debug("inserting record :{} in topic :{}", msg, workFlowTopic);
		kafkaTemplate.send(workFlowTopic, msg);
	}

	public List<Map<String, Object>> getAll(String formName, String tenantID, String userID, Boolean applyAuth)
			throws JSONException, IOException {
		formName = formName.toLowerCase();
		boolean isexist = isCollectionExist(formName);
		if (isexist) {
			// BasicDBObject whereQuery = new BasicDBObject();
			// whereQuery.put("userID", userID);
			MongoCollection<Document> collection = mongoTemplate.getDb().getCollection(formName.toLowerCase());
			FindIterable<Document> cursor = collection.find().sort(new BasicDBObject("createdDate", -1));
			log.debug("cursor:{}", cursor);
			Iterator iterator = cursor.iterator();
			List<Map<String, Object>> documents = new ArrayList<>();
			while (iterator.hasNext()) {
				Document doc = (Document) iterator.next();
//                @SuppressWarnings("deprecation")
				String jsonDoc = com.mongodb.util.JSON.serialize(doc);
//                @SuppressWarnings("unchecked")
				Map<String, Object> responseObj = new ObjectMapper().readValue(jsonDoc, Map.class);
//                Map<String, Object> mapObj = (Map<String, Object>) responseObj.get("_id");
//                responseObj.put("_id", mapObj.get("$oid").toString());
				if (responseObj != null) {
					if (applyAuth) {
						Map<String, Object> recPermissions = new HashMap<>();
						if (urv.isUserSuperAdmin(userID)) {
//					recPermissions.put("update", true);
//					recPermissions.put("reports", true);
//					recPermissions.put("sendEmail", true);
//					recPermissions.put("export", true);
//					recPermissions.put("viewLogs", true);
//					recPermissions.put("relatedRecords", true);
							recPermissions.put("superAdmin", true);
							responseObj.put("recPermissions", recPermissions);
							documents.add(responseObj);
						} else {
							recPermissions.put("superAdmin", false);
							DataRequest dataRequest = new DataRequest();
							dataRequest.setFormName(formName);
							dataRequest.setData(responseObj);
							dataRequest.setTenantID(tenantID);
							dataRequest.setUserID(userID);

							if (urv.validateUserViewPermissions(dataRequest) != null) {
								if (urv.validateUserUpdatePermissions(dataRequest) != null) {

									recPermissions.put("update", true);
								} else {
									recPermissions.put("update", false);
								}
								Map<String, Object> updatedData = urv.validateFieldViewPermissions(dataRequest);
								Role.FormPermission formPermission = urv.validateUserViewPermissions(dataRequest);
								recPermissions.put("reports", formPermission.isReports());
								recPermissions.put("sendEmail", formPermission.isSendEmail());
								recPermissions.put("export", formPermission.isExport());
								recPermissions.put("viewLogs", formPermission.isViewLogs());
								recPermissions.put("relatedRecords", formPermission.isRelatedRecords());
								updatedData.put("recPermissions", recPermissions);

								documents.add(updatedData);
							}
						}
					} else {
						documents.add(responseObj);
					}
				}
			}
			return documents;
		} else {
			// createCollection(dataRequest.getFormName().toLowerCase());
		}
		return null;
	}

	public Map<String, Object> getInfoByID(String id, String formName, String userID, String tenantID)
			throws JSONException, IOException {
		formName = formName.toLowerCase();
		boolean isexist = isCollectionExist(formName);
		if (isexist) {
			BasicDBObject whereQuery = new BasicDBObject();
			// whereQuery.put("userID", userID);
			whereQuery.put("_id", id);
			MongoCollection<Document> collection = mongoTemplate.getDb().getCollection(formName.toLowerCase());
			FindIterable<Document> cursor = collection.find(whereQuery);

			for (Document doc : cursor) {
//                @SuppressWarnings("deprecation")
				String jsonDoc = com.mongodb.util.JSON.serialize(doc);
//                @SuppressWarnings("unchecked")
				Map<String, Object> responseObj = new ObjectMapper().readValue(jsonDoc, Map.class);
//                Map<String, Object> fd = (Map<String, Object>) ff.get("_id");
//                ff.put("_id", fd.get("$oid").toString());

				DataRequest dataRequest = new DataRequest();
				dataRequest.setFormName(formName);
				dataRequest.setData(responseObj);
				dataRequest.setUserID(userID);
				dataRequest.setTenantID(tenantID);
				if(urv.isUserMasterAdmin(userID)) {
					responseObj.put("masterAdmin", true);
					return responseObj;
				} else if (!urv.isUserSuperAdmin(userID)) {
					Role.FormPermission formPermission = urv.validateUserViewPermissions(dataRequest);
					if (formPermission != null) {
						if (urv.validateUserUpdatePermissions(dataRequest) != null) {

							responseObj.put("update", true);
						} else {
							responseObj.put("update", false);
						}
						if (urv.validateUserCreatePermissions(dataRequest) != null) {

							responseObj.put("create", true);
						} else {
							responseObj.put("create", false);
						}
						Map<String, FieldAuth> fieldPermissions = urv.validateUserFieldPermissions(dataRequest);
						responseObj.put("fieldPermissions", fieldPermissions);
						responseObj.put("reports", formPermission.isReports());
						responseObj.put("sendEmail", formPermission.isSendEmail());
						responseObj.put("export", formPermission.isExport());
						responseObj.put("viewLogs", formPermission.isViewLogs());
						responseObj.put("relatedRecords", formPermission.isRelatedRecords());
						responseObj.put("superAdmin", false);

						return responseObj;
					}
				} else {
					responseObj.put("superAdmin", true);
					return responseObj;
				}

			}

		} else {
			// createCollection(dataRequest.getFormName().toLowerCase());
		}
		return null;
	}

	public Map<String, Object> getByID(String id, String formName) throws JSONException, IOException {
		boolean isexist = isCollectionExist(formName);
		if (isexist) {
			BasicDBObject whereQuery = new BasicDBObject();
			// whereQuery.put("userID", userID);
			whereQuery.put("_id", id);
			MongoCollection<Document> collection = mongoTemplate.getDb().getCollection(formName.toLowerCase());
			FindIterable<Document> cursor = collection.find(whereQuery);
			log.debug("cursor:{}", cursor);
			for (Document doc : cursor) {
//                @SuppressWarnings("deprecation")
				String jsonDoc = com.mongodb.util.JSON.serialize(doc);
//                @SuppressWarnings("unchecked")
				Map<String, Object> responseObj = new ObjectMapper().readValue(jsonDoc, Map.class);
//                Map<String, Object> fd = (Map<String, Object>) ff.get("_id");
//                ff.put("_id", fd.get("$oid").toString());
				return responseObj;

			}

		}
		return null;
	}

	public List<Map<String, Object>> getAllByFormName(String formName) throws JSONException, IOException {
		boolean isexist = isCollectionExist(formName);
		List<Map<String, Object>> formList = new ArrayList<>();
		if (isexist) {

			MongoCollection<Document> collection = mongoTemplate.getDb().getCollection(formName.toLowerCase());
			FindIterable<Document> cursor = collection.find();
			log.debug("cursor:{}", cursor);
			for (Document doc : cursor) {
				@SuppressWarnings("deprecation")
				String jsonDoc = com.mongodb.util.JSON.serialize(doc);
				@SuppressWarnings("unchecked")
				Map<String, Object> ff = new ObjectMapper().readValue(jsonDoc, Map.class);
//                Map<String, Object> fd = (Map<String, Object>) ff.get("_id");
//                ff.put("_id", fd.get("$oid").toString());
				formList.add(ff);

			}

		}
		return formList;
	}

	public void delete(String id, String formName, String tenantID, String isSubscriptionExpired) throws JSONException, IOException, KpoDataException {
		mongoDBDistint.packageIsExpired(isSubscriptionExpired);
		boolean isexist = isCollectionExist(formName);
		if (isexist) {
			BasicDBObject whereQuery = new BasicDBObject();
			// whereQuery.put("userID", userID);
			whereQuery.put("_id", id);
			MongoCollection<Document> collection = mongoTemplate.getDb().getCollection(formName.toLowerCase());
			collection.findOneAndDelete(whereQuery);
			if (formName.equalsIgnoreCase("employee")) {

				cacheInstance.deleteKey("employeeInfo", tenantID + "@" + id);
			}
		} else {
			log.error("Form Not exist");
		}

	}

	private boolean isPwdField(String refField) {
		if (redisTempalte.opsForHash().hasKey("fieldInfo", refField)) {
			FormField formField = (FormField) redisTempalte.opsForHash().get("fieldInfo", refField);
			if (formField.getType().equalsIgnoreCase("password")) {
				return true;
			}

		}
		return false;
	}

	private String encriptPwd(String password) {
		return encoder.encode(password);
	}

	private boolean isMapOrNot(Object object) {

		if (object instanceof List) {
			if (((List) object).size() > 0 && (((List) object).get(0) instanceof Map)) {
				return true;
			}
		}

		return false;
	}

	private Double validateLeaveRequest(DataRequest dataRequest) throws KpoDataException {
		long from = 0l;
		long to = 0l;
		String userID = dataRequest.getUserID();

		if (CollectionUtils.isEmpty(lmssi.getleaveTypes(userID))) {
			log.debug("No Leavetypes Available for userID: {}", userID);
			ResponseMessage responseMessage = new ResponseMessage();
			HashMap<String, Object> errorObj = new HashMap<>();
			responseMessage.setData(errorObj);
			responseMessage.setHttpStatus(HttpStatus.BAD_REQUEST);
			responseMessage.setMsg("No Leavetypes Available for userID");
			responseMessage.setCode(400);
			throw new KpoDataException("No Leavetypes Available for userID", responseMessage);
		}

		if (dataRequest.getData().containsKey("From")) {
			from = (long) dataRequest.getData().get("From");
		}
		if (dataRequest.getData().containsKey("To")) {
			to = (long) dataRequest.getData().get("To");
		}
		String leaveType = dataRequest.getData().get("Leave_Type").toString();
		Optional<LeaveTypes> leaveTypeObj = leaveTypeRepo.findById(leaveType);
		if (leaveTypeObj.isPresent()) {
			// leave submit restrictions
			LeaveTypes lt = leaveTypeObj.get();
			if (lt.isEvidence() && !dataRequest.getData().containsKey("evidence")) {
				ResponseMessage responseMessage = new ResponseMessage();
				HashMap<String, Object> errorObj = new HashMap<>();
				responseMessage.setData(errorObj);
				responseMessage.setHttpStatus(HttpStatus.BAD_REQUEST);
				responseMessage.setMsg("Evidence is required");
				responseMessage.setCode(400);
				throw new KpoDataException("Evidence is required", responseMessage);
			}

			LocalDate currentDateTime = LocalDate.now();

			LocalDate fromDate = LocalDateTime
					.ofInstant(Instant.ofEpochMilli(from), ZoneId.of(ZoneId.systemDefault().getId())).toLocalDate();
			LocalDate toDate = LocalDateTime
					.ofInstant(Instant.ofEpochMilli(to), ZoneId.of(ZoneId.systemDefault().getId())).toLocalDate();
			Period duration = Period.between(currentDateTime, fromDate);

			long beforeDays = duration.getDays();
//			if (isDatesExistsInDB(fromDate, toDate, userID)) {
//				log.debug("To-Date should not be  lessthan From-Date: {}");
//				ResponseMessage responseMessage = new ResponseMessage();
//				HashMap<String, Object> errorObj = new HashMap<>();
//				responseMessage.setData(errorObj);
//				responseMessage.setHttpStatus(HttpStatus.BAD_REQUEST);
//				responseMessage.setMsg("Dustplicate Request");
//				responseMessage.setCode(400);
//				throw new KpoDataException("Dustplicate Request", responseMessage);
//			}
//			if (isValidDays(fromDate, toDate)) {
//				log.debug("To-Date should not be  lessthan From-Date: {}");
//				ResponseMessage responseMessage = new ResponseMessage();
//				HashMap<String, Object> errorObj = new HashMap<>();
//				responseMessage.setData(errorObj);
//				responseMessage.setHttpStatus(HttpStatus.BAD_REQUEST);
//				responseMessage.setMsg("To-Date should not be  lessthan From-Date");
//				responseMessage.setCode(400);
//				throw new KpoDataException("To-Date should not be  lessthan From-Date", responseMessage);
//			}
			if (lt.getBeforeDays() != null && lt.getBeforeDays().longValue() < beforeDays) {
				ResponseMessage responseMessage = new ResponseMessage();
				HashMap<String, Object> data = new HashMap<>();
				responseMessage.setData(data);
				responseMessage.setHttpStatus(HttpStatus.OK);
				responseMessage.setCode(200);
				responseMessage.setMsg("Max submitted before days :" + lt.getBeforeDays().longValue());
				throw new KpoDataException("NOT ALLOWED", responseMessage);
			}
			
			Optional<Employee> employee = employeeRepo.findById(userID);
			List<Long> holidays = null;
			if(employee.isPresent()) {
				holidays = holidayRepo.findByActiveTrue().stream()
						.filter(hl -> hl.getApplicable_Locations().contains(employee.get().getLocation()))
						.map(Holidays::getDate)
						.collect(Collectors.toList());
			}			
			List<LocalDate> hDays = new ArrayList<>();
			holidays.forEach((hd) -> {
				hDays.add(LocalDateTime.ofInstant(Instant.ofEpochMilli(hd), ZoneId.systemDefault()).toLocalDate());
			});	
			
			float days = countLeaveDaysBetween(fromDate, toDate, hDays);
			
			if(days > 0) {
				int fromDay = (int) dataRequest.getData().get("FromDay");
				int toDay = (int) dataRequest.getData().get("ToDay");
				boolean isFrmDtHldyWknd = hDays.contains(fromDate) || fromDate.getDayOfWeek() == DayOfWeek.SATURDAY
						|| fromDate.getDayOfWeek() == DayOfWeek.SUNDAY;
				boolean isToDtHldyWknd = hDays.contains(toDate) || toDate.getDayOfWeek() == DayOfWeek.SATURDAY
						|| toDate.getDayOfWeek() == DayOfWeek.SUNDAY;
				
				if(fromDate.isEqual(toDate) && (fromDay == 2 && toDay == 2) 
						&& (!isFrmDtHldyWknd && !isToDtHldyWknd)) {
					days -= 0.5;
				}  else if(!fromDate.isEqual(toDate) && (fromDay == 2 && toDay == 2) 
						&& (!isFrmDtHldyWknd && !isToDtHldyWknd)) {
					days -= 1.0;
				} else if(!fromDate.isEqual(toDate) && (fromDay == 2 || toDay == 2) 
						&& (!isFrmDtHldyWknd || !isToDtHldyWknd)) {
					days -= 0.5;
				}
			}			
			
			if (days == 0) {
				ResponseMessage responseMessage = new ResponseMessage();
				HashMap<String, Object> data = new HashMap<>();
				responseMessage.setData(data);
				responseMessage.setHttpStatus(HttpStatus.BAD_REQUEST);
				responseMessage.setCode(400);
				responseMessage.setMsg("You have applied leave on holiday or weekend");
				throw new KpoDataException("Leave not count on holiday or weekend", responseMessage);
			} else {
				return Double.valueOf(days);
			}
			
//			long diff = 0l;
//			float days = 0.0f;
//			if (lt.isExcludeHolidays()) {
//				List<LocalDate> remainDays = excludeHolidays(fromDate, toDate);
//				days = remainDays.size();
//				if (days == 0) {
//					ResponseMessage responseMessage = new ResponseMessage();
//					HashMap<String, Object> data = new HashMap<>();
//					responseMessage.setData(data);
//					responseMessage.setHttpStatus(HttpStatus.BAD_REQUEST);
//					responseMessage.setCode(400);
//					responseMessage.setMsg("You have applied leave on holiday");
//					throw new KpoDataException("Leave not count on holiday", responseMessage);
//				}
//			} else if (lt.isExcludeWeekends()) {
//				List<LocalDate> remainDays = excludeWeekends(fromDate, toDate);
//				days = remainDays.size();
//				if (days == 0) {
//					ResponseMessage responseMessage = new ResponseMessage();
//					HashMap<String, Object> data = new HashMap<>();
//					responseMessage.setData(data);
//					responseMessage.setHttpStatus(HttpStatus.BAD_REQUEST);
//					responseMessage.setCode(400);
//					responseMessage.setMsg("You have applied leave on weekend");
//					throw new KpoDataException("Leave not count on weekend", responseMessage);
//				}
//			} else {
//				// duration.toDays();
//				diff = Math.abs(to - from);
//				days = (diff / (1000 * 60 * 60 * 24));
//				days += 1;
//			}
			// TimeUnit.DAYS.convert(diff, TimeUnit.MILLISECONDS);
//			final float appDays = days;

//			if (lmssi.isEmployeeInProbitionPeriod(userID, lt)) {
//				LeaveInfo leaveInfo = lmssi.getLeaveBalance(userID);
//				if (leaveInfo != null && CollectionUtils.isNotEmpty(leaveInfo.getBalanceByType())) {
//					LeaveInfo lInfo = lmssi.getLeaveBalance(userID);
//					List<LeaveBalance> bal = lInfo.getBalanceByType();
//					float ltBal = 0.0f;
//					for (LeaveBalance lb : bal) {
//						if (lb.getLeaveTypeID().equalsIgnoreCase(lt.getLeaveTypeID())) {
//							if (ltBal > lt.getProbationPeriodValue()) {
//								ResponseMessage responseMessage = new ResponseMessage();
//								HashMap<String, Object> data = new HashMap<>();
//								responseMessage.setData(data);
//								responseMessage.setHttpStatus(HttpStatus.OK);
//								responseMessage.setCode(200);
//								responseMessage
//										.setMsg("Max allowed days in probition period:" + lt.getProbationPeriodValue());
//								throw new KpoDataException("Max allowed days in probition period", responseMessage);
//							}
//						}
//					}
//
//				} else {
//					if (lt.getProbationPeriodValue() != null && days > lt.getProbationPeriodValue()) {
//						ResponseMessage responseMessage = new ResponseMessage();
//						HashMap<String, Object> data = new HashMap<>();
//						responseMessage.setData(data);
//						responseMessage.setHttpStatus(HttpStatus.OK);
//						responseMessage.setCode(200);
//						responseMessage.setMsg("Max allowed days in probition period:" + lt.getProbationPeriodValue());
//						throw new KpoDataException("Max allowed days in probition period", responseMessage);
//					}
//				}
//			}
//			dataRequest.getData().put("noOfDays", days);
//			if (lt.getMaxConsecutiveDays() != null && days > lt.getMaxConsecutiveDays().longValue()) {
//				ResponseMessage responseMessage = new ResponseMessage();
//				HashMap<String, Object> data = new HashMap<>();
//				responseMessage.setData(data);
//				responseMessage.setHttpStatus(HttpStatus.OK);
//				responseMessage.setCode(200);
//				responseMessage.setMsg("Max Consecutive days allowed:" + lt.getMaxConsecutiveDays().longValue());
//				throw new KpoDataException("NOT ALLOWED", responseMessage);
//			}

		} else {
			ResponseMessage responseMessage = new ResponseMessage();
			HashMap<String, Object> data = new HashMap<>();
			responseMessage.setData(data);
			responseMessage.setHttpStatus(HttpStatus.NO_CONTENT);
			responseMessage.setCode(204);
			responseMessage.setMsg("LeaveType not exist");
			throw new KpoDataException("NOT FOUND", responseMessage);
		}

	}

	private List<LocalDate> excludeHolidays(LocalDate fromDate, LocalDate toDate) {
		List<Long> holidays = holidayRepo.findByActiveTrue().stream().map(Holidays::getDate)
				.collect(Collectors.toList());

		List<LocalDate> hDays = new ArrayList<>();
		holidays.forEach((hd) -> {
			hDays.add(LocalDateTime.ofInstant(Instant.ofEpochMilli(hd), ZoneId.systemDefault()).toLocalDate());
		});
		return getDaysBetween(fromDate, toDate).stream().filter(date -> !hDays.contains(date))
				.collect(Collectors.toList());
	}

	private List<LocalDate> excludeWeekends(LocalDate fromDate, LocalDate toDate) {
		return getDaysBetween(fromDate, toDate).stream()
				.filter(date -> date.getDayOfWeek() == DayOfWeek.SATURDAY || date.getDayOfWeek() == DayOfWeek.SUNDAY)
				.collect(Collectors.toList());

	}

	public List<LocalDate> getDaysBetween(LocalDate startDate, LocalDate endDate) {
		long numOfDaysBetween = ChronoUnit.DAYS.between(startDate, endDate);
		return IntStream.iterate(0, i -> i + 1).limit(numOfDaysBetween).mapToObj(i -> startDate.plusDays(i))
				.collect(Collectors.toList());
	}

	private boolean isValidDays(LocalDate fromDate, LocalDate toDate) {
		return toDate.compareTo(fromDate) < 0;
	}

	private boolean isDatesExistsInDB(LocalDate fromDate, LocalDate toDate, String userID) {
		boolean exist = false;
//		List<Leave> leaves = leaveRepo.findByUserID(userID);
//		List<Leave> appLeaves = leaves.stream().filter(r -> r.getStatus() != 3 || r.getStatus() != 4)
//				.collect(Collectors.toList());
//		for (Leave l : appLeaves) {
//			LocalDate fDate = LocalDateTime
//					.ofInstant(Instant.ofEpochMilli(l.getFrom()), ZoneId.of(ZoneId.systemDefault().getId()))
//					.toLocalDate();
//			LocalDate tDate = LocalDateTime
//					.ofInstant(Instant.ofEpochMilli(l.getTo()), ZoneId.of(ZoneId.systemDefault().getId()))
//					.toLocalDate();
//
//			if (fromDate.compareTo(fDate) == 0 || fromDate.compareTo(tDate) == 0) {
//				exist = true;
//				break;
//
//			}
//			if (toDate.compareTo(fDate) == 0 || toDate.compareTo(tDate) == 0) {
//				exist = true;
//				break;
//			}
//
//			if (fromDate.isAfter(fromDate) || fromDate.isBefore(tDate)) {
//				exist = true;
//				break;
//			}
//			if (toDate.isAfter(fromDate) || toDate.isBefore(tDate)) {
//				exist = true;
//				break;
//			}
//
//		}
		return exist;
	}

	public List<HolidayDto> getHolidayList(String userId) {
		List<HolidayDto> listOfHoliday = new ArrayList<>();
		Optional<Employee> employee = employeeRepo.findById(userId);
		List<String> locationList = new ArrayList<>();
		locationList.add(employee.get().getLocation());
		List<Holidays> holidayList = holidayRepo.findByActiveTrue();
		for (Holidays holidays : holidayList) {
			if (holidays.getApplicable_Locations().contains(employee.get().getLocation())) {
				HolidayDto holidayDto = new HolidayDto();
				holidayDto.setTitle(holidays.getName());
				holidayDto.setDate(new Date(holidays.getDate()));
				listOfHoliday.add(holidayDto);
			}
		}
		return listOfHoliday;
	}	  
	 
	private long countLeaveDaysBetween(LocalDate startDate, LocalDate endDate, List<LocalDate> hDays) {	
			
		if (startDate == null || endDate == null || hDays == null) {
			throw new IllegalArgumentException("Invalid method argument(s) to countBusinessDaysBetween(" + startDate
					+ "," + endDate + "," + hDays + ")");
		}		
		Optional<List<LocalDate>> hlDays = Optional.of(hDays);

		Predicate<LocalDate> isHoliday = date -> hlDays.isPresent() ? hlDays.get().contains(date) : false;
		Predicate<LocalDate> isWeekend = date -> date.getDayOfWeek() == DayOfWeek.SATURDAY
				|| date.getDayOfWeek() == DayOfWeek.SUNDAY;
		long daysBetween = ChronoUnit.DAYS.between(startDate, endDate.plusDays(1));
		long businessDays = Stream.iterate(startDate, date -> date.plusDays(1)).limit(daysBetween)
				.filter(isHoliday.or(isWeekend).negate()).count();
		
		return businessDays;
	}
	

}
