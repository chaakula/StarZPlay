/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.kpodata.controller;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.json.JSONException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.kpodata.dto.HolidayDto;
import com.kpodata.exception.KpoDataException;
import com.kpodata.model.DataRequest;
import com.kpodata.model.ResponseMessage;
import com.kpodata.service.impl.DataOperationServiceImpl;
import com.kpodata.util.UserRoleValidation;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import lombok.extern.slf4j.Slf4j;

/**
 *
 * @author pasrinivas
 */
@RestController
@RequestMapping(value = "/data")
@CrossOrigin(origins = "*")
@Slf4j
@Api(value = "/data", tags = "Data Operations", description = "Data Operations")
public class DataController {

	@Autowired
	private
	dataOperationService;
	@Autowired
	UserRoleValidation userRoleValidation;

	@ApiResponses(value = {
			@ApiResponse(code = 201, message = "Created.{The request has been fulfilled, resulting in the creation of a new resource.}"),
			@ApiResponse(code = 400, message = "Bad Request.{The requested resource could not be found}"),
			@ApiResponse(code = 405, message = "Method Not Allowed.{A request method is not supported for the requested resource}"),
			@ApiResponse(code = 406, message = "Not Acceptable.{The requested resource is capable of generating only content not acceptable according to the Accept headers sent in the request}"),
			@ApiResponse(code = 409, message = "Conflict.{the request could not be processed because of conflict in the current state of the resource, such as an edit conflict between multiple simultaneous updates}"),
			@ApiResponse(code = 415, message = "Unsupported Media Type.{The request entity has a media type which the server or resource does not support}"),
			@ApiResponse(code = 417, message = "Expectation Failed.{The server cannot meet the requirements of the Expect request-header field}"),
			@ApiResponse(code = 440, message = "Login Time-out.{The client's session has expired and must log in again}"),
			@ApiResponse(code = 499, message = "Token Required.{Token is required but was not submitted}"),
			@ApiResponse(code = 500, message = "Internal Server Error.{An unexpected condition was encountered}") })
	@PostMapping(produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
	@ApiOperation(value = "Post Data from specified form")
	public ResponseEntity<ResponseMessage> postData(
			@ApiParam(value = "isSubscriptionExpired") @RequestHeader(required = false) String isSubscriptionExpired,
			@ApiParam(value = "tenantID") @RequestHeader(required = true) String tenantID, HttpServletRequest request,
			@RequestBody DataRequest dataRequest) throws KpoDataException, IOException, JSONException {
		String userID = request.getAttribute("userID").toString();
		dataRequest.setTenantID(tenantID);
		dataRequest.setUserID(userID);

//		if (request.getHeader("origin") != null) {
//			dataRequest.setHost(request.getHeader("origin"));
//		} else {
//			dataRequest.setHost("localhost");
//		}
		String id = dataOperationService.insertData(dataRequest, isSubscriptionExpired);
		ResponseMessage responseMessage = new ResponseMessage();
		HashMap<String, Object> data = new HashMap<>();
		data.put("id", id);
		responseMessage.setData(data);
		responseMessage.setHttpStatus(HttpStatus.CREATED);
		responseMessage.setMsg("Successfully created");
		responseMessage.setCode(201);
		return new ResponseEntity<>(responseMessage, HttpStatus.CREATED);

	}

	@ApiResponses(value = {
			@ApiResponse(code = 201, message = "Created.{The request has been fulfilled, resulting in the creation of a new resource.}"),
			@ApiResponse(code = 400, message = "Bad Request.{The requested resource could not be found}"),
			@ApiResponse(code = 405, message = "Method Not Allowed.{A request method is not supported for the requested resource}"),
			@ApiResponse(code = 406, message = "Not Acceptable.{The requested resource is capable of generating only content not acceptable according to the Accept headers sent in the request}"),
			@ApiResponse(code = 409, message = "Conflict.{the request could not be processed because of conflict in the current state of the resource, such as an edit conflict between multiple simultaneous updates}"),
			@ApiResponse(code = 415, message = "Unsupported Media Type.{The request entity has a media type which the server or resource does not support}"),
			@ApiResponse(code = 417, message = "Expectation Failed.{The server cannot meet the requirements of the Expect request-header field}"),
			@ApiResponse(code = 440, message = "Login Time-out.{The client's session has expired and must log in again}"),
			@ApiResponse(code = 499, message = "Token Required.{Token is required but was not submitted}"),
			@ApiResponse(code = 500, message = "Internal Server Error.{An unexpected condition was encountered}") })
	@PutMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	@ApiOperation(value = "Put Data from specified form")
	public ResponseEntity<ResponseMessage> updateData(
			@ApiParam(value = "isSubscriptionExpired") @RequestHeader(required = false) String isSubscriptionExpired,
			@ApiParam(value = "tenantID") @RequestHeader(required = true) String tenantID, HttpServletRequest request,
			@RequestBody DataRequest dataObj) throws KpoDataException, IOException, JSONException {
		String userID = request.getAttribute("userID").toString();
		dataObj.setTenantID(tenantID);
		dataObj.setUserID(userID);

//		if (request.getHeader("origin") != null) {
//			dataObj.setHost(request.getHeader("origin"));
//		} else {
//			dataObj.setHost("localhost");
//		}
		Map<String, Object> updatedData = dataOperationService.updateData(dataObj, isSubscriptionExpired);
		ResponseMessage responseMessage = new ResponseMessage();
		HashMap<String, Object> data = new HashMap<>();
		data.put("updated", updatedData);
		responseMessage.setData(data);
		responseMessage.setHttpStatus(HttpStatus.OK);
		responseMessage.setMsg("Successfully updated");
		responseMessage.setCode(202);
		return new ResponseEntity<>(responseMessage, HttpStatus.OK);

	}

	@ApiResponses(value = {
			@ApiResponse(code = 201, message = "Created.{The request has been fulfilled, resulting in the creation of a new resource.}"),
			@ApiResponse(code = 400, message = "Bad Request.{The requested resource could not be found}"),
			@ApiResponse(code = 405, message = "Method Not Allowed.{A request method is not supported for the requested resource}"),
			@ApiResponse(code = 406, message = "Not Acceptable.{The requested resource is capable of generating only content not acceptable according to the Accept headers sent in the request}"),
			@ApiResponse(code = 409, message = "Conflict.{the request could not be processed because of conflict in the current state of the resource, such as an edit conflict between multiple simultaneous updates}"),
			@ApiResponse(code = 415, message = "Unsupported Media Type.{The request entity has a media type which the server or resource does not support}"),
			@ApiResponse(code = 417, message = "Expectation Failed.{The server cannot meet the requirements of the Expect request-header field}"),
			@ApiResponse(code = 440, message = "Login Time-out.{The client's session has expired and must log in again}"),
			@ApiResponse(code = 499, message = "Token Required.{Token is required but was not submitted}"),
			@ApiResponse(code = 500, message = "Internal Server Error.{An unexpected condition was encountered}") })
	@GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
	@ApiOperation(value = "Get Data from specified form by user")
	public ResponseEntity<ResponseMessage> getByUser(
			@ApiParam(value = "tenantID") @RequestHeader(required = true) String tenantID,
			@ApiParam(value = "applyAuth") @RequestHeader(required = false, defaultValue = "true") String applyAuth,
			HttpServletRequest request, @RequestHeader(required = true) String formName)
			throws KpoDataException, IOException, JSONException {
		String userID = request.getAttribute("userID").toString();
		Boolean authEnable = Boolean.valueOf(applyAuth);
		List<Map<String, Object>> list = dataOperationService.getAll(formName, tenantID, userID, authEnable);
		ResponseMessage responseMessage = new ResponseMessage();
		HashMap<String, Object> data = new HashMap<>();
		data.put("response", list);
		responseMessage.setData(data);
		responseMessage.setHttpStatus(HttpStatus.OK);
		responseMessage.setMsg("List of Data");
		responseMessage.setCode(200);
		return new ResponseEntity<>(responseMessage, HttpStatus.OK);

	}

//    @ApiResponses(value = {
//        @ApiResponse(code = 201, message = "Created.{The request has been fulfilled, resulting in the creation of a new resource.}"),
//        @ApiResponse(code = 400, message = "Bad Request.{The requested resource could not be found}"),
//        @ApiResponse(code = 405, message = "Method Not Allowed.{A request method is not supported for the requested resource}"),
//        @ApiResponse(code = 406, message = "Not Acceptable.{The requested resource is capable of generating only content not acceptable according to the Accept headers sent in the request}"),
//        @ApiResponse(code = 409, message = "Conflict.{the request could not be processed because of conflict in the current state of the resource, such as an edit conflict between multiple simultaneous updates}"),
//        @ApiResponse(code = 415, message = "Unsupported Media Type.{The request entity has a media type which the server or resource does not support}"),
//        @ApiResponse(code = 417, message = "Expectation Failed.{The server cannot meet the requirements of the Expect request-header field}"),
//        @ApiResponse(code = 440, message = "Login Time-out.{The client's session has expired and must log in again}"),
//        @ApiResponse(code = 499, message = "Token Required.{Token is required but was not submitted}"),
//        @ApiResponse(code = 500, message = "Internal Server Error.{An unexpected condition was encountered}")
//    })
//    @GetMapping(value = "/{docID}", produces = MediaType.APPLICATION_JSON_VALUE)
//    @ApiOperation(value = "Get Data from specified form and ID")
//    public ResponseEntity getByID(@ApiParam(value = "tenantID") @RequestHeader(required = true) String tenantID, @RequestHeader(required = true) String formName,
//            @PathVariable("docID") String docID, HttpServletRequest request) throws KpoDataException, IOException, JSONException {
//        String userId = request.getAttribute("userID").toString();
//        Map<String, Object> doc = dataOperationService.getByID(docID, formName);
//
//        ResponseMessage responseMessage = new ResponseMessage();
//        HashMap<String, Object> data = new HashMap<>();
//        data.put("response", doc);
//        responseMessage.setData(data);
//        responseMessage.setHttpStatus(HttpStatus.OK);
//        responseMessage.setMsg("Data");
//        responseMessage.setCode(200);
//        return new ResponseEntity<>(responseMessage, HttpStatus.OK);
//
//    }
	@ApiResponses(value = {
			@ApiResponse(code = 201, message = "Created.{The request has been fulfilled, resulting in the creation of a new resource.}"),
			@ApiResponse(code = 400, message = "Bad Request.{The requested resource could not be found}"),
			@ApiResponse(code = 405, message = "Method Not Allowed.{A request method is not supported for the requested resource}"),
			@ApiResponse(code = 406, message = "Not Acceptable.{The requested resource is capable of generating only content not acceptable according to the Accept headers sent in the request}"),
			@ApiResponse(code = 409, message = "Conflict.{the request could not be processed because of conflict in the current state of the resource, such as an edit conflict between multiple simultaneous updates}"),
			@ApiResponse(code = 415, message = "Unsupported Media Type.{The request entity has a media type which the server or resource does not support}"),
			@ApiResponse(code = 417, message = "Expectation Failed.{The server cannot meet the requirements of the Expect request-header field}"),
			@ApiResponse(code = 440, message = "Login Time-out.{The client's session has expired and must log in again}"),
			@ApiResponse(code = 499, message = "Token Required.{Token is required but was not submitted}"),
			@ApiResponse(code = 500, message = "Internal Server Error.{An unexpected condition was encountered}") })
	@GetMapping(value = "/{docID}", produces = MediaType.APPLICATION_JSON_VALUE)
	@ApiOperation(value = "Get Data from specified form and ID")
	public ResponseEntity<ResponseMessage> authorizationInfoByID(
			@ApiParam(value = "tenantID") @RequestHeader(required = true) String tenantID,
			@RequestHeader(required = true) String formName, @PathVariable("docID") String docID,
			HttpServletRequest request) throws KpoDataException, IOException, JSONException {
		String userId = request.getAttribute("userID").toString();
		Map<String, Object> doc = dataOperationService.getInfoByID(docID, formName, userId, tenantID);

		ResponseMessage responseMessage = new ResponseMessage();
		HashMap<String, Object> data = new HashMap<>();
		data.put("response", doc);
		responseMessage.setData(data);
		responseMessage.setHttpStatus(HttpStatus.OK);
		responseMessage.setMsg("Data");
		responseMessage.setCode(200);
		return new ResponseEntity<>(responseMessage, HttpStatus.OK);

	}

	@ApiResponses(value = {
			@ApiResponse(code = 201, message = "Created.{The request has been fulfilled, resulting in the creation of a new resource.}"),
			@ApiResponse(code = 400, message = "Bad Request.{The requested resource could not be found}"),
			@ApiResponse(code = 405, message = "Method Not Allowed.{A request method is not supported for the requested resource}"),
			@ApiResponse(code = 406, message = "Not Acceptable.{The requested resource is capable of generating only content not acceptable according to the Accept headers sent in the request}"),
			@ApiResponse(code = 409, message = "Conflict.{the request could not be processed because of conflict in the current state of the resource, such as an edit conflict between multiple simultaneous updates}"),
			@ApiResponse(code = 415, message = "Unsupported Media Type.{The request entity has a media type which the server or resource does not support}"),
			@ApiResponse(code = 417, message = "Expectation Failed.{The server cannot meet the requirements of the Expect request-header field}"),
			@ApiResponse(code = 440, message = "Login Time-out.{The client's session has expired and must log in again}"),
			@ApiResponse(code = 499, message = "Token Required.{Token is required but was not submitted}"),
			@ApiResponse(code = 500, message = "Internal Server Error.{An unexpected condition was encountered}") })
	@DeleteMapping(value = "/{formId}", produces = MediaType.APPLICATION_JSON_VALUE)
	@ApiOperation(value = "Get Data from specified form and ID")
	public ResponseEntity<ResponseMessage> delete(
			@ApiParam(value = "isSubscriptionExpired") @RequestHeader(required = false) String isSubscriptionExpired,
			@ApiParam(value = "tenantID") @RequestHeader(required = true) String tenantID,
			@RequestHeader(required = true) String formName, @PathVariable("formId") String formId,
			HttpServletRequest request) throws KpoDataException, IOException, JSONException {
		dataOperationService.delete(formId, formName, tenantID, isSubscriptionExpired);
		ResponseMessage responseMessage = new ResponseMessage();
		HashMap<String, Object> data = new HashMap<>();
		data.put("id", formId);
		responseMessage.setData(data);
		responseMessage.setHttpStatus(HttpStatus.OK);
		responseMessage.setMsg("Deleted Successfully");
		responseMessage.setCode(200);
		return new ResponseEntity<>(responseMessage, HttpStatus.OK);

	}


	@ApiResponses(value = {
			@ApiResponse(code = 201, message = "Created.{The request has been fulfilled, resulting in the creation of a new resource.}"),
			@ApiResponse(code = 400, message = "Bad Request.{The requested resource could not be found}"),
			@ApiResponse(code = 405, message = "Method Not Allowed.{A request method is not supported for the requested resource}"),
			@ApiResponse(code = 406, message = "Not Acceptable.{The requested resource is capable of generating only content not acceptable according to the Accept headers sent in the request}"),
			@ApiResponse(code = 409, message = "Conflict.{the request could not be processed because of conflict in the current state of the resource, such as an edit conflict between multiple simultaneous updates}"),
			@ApiResponse(code = 415, message = "Unsupported Media Type.{The request entity has a media type which the server or resource does not support}"),
			@ApiResponse(code = 417, message = "Expectation Failed.{The server cannot meet the requirements of the Expect request-header field}"),
			@ApiResponse(code = 440, message = "Login Time-out.{The client's session has expired and must log in again}"),
			@ApiResponse(code = 499, message = "Token Required.{Token is required but was not submitted}"),
			@ApiResponse(code = 500, message = "Internal Server Error.{An unexpected condition was encountered}") })
	@GetMapping(value = "/getHolidayList", produces = MediaType.APPLICATION_JSON_VALUE)
	@ApiOperation(value = "Get Holiday List")
	public ResponseEntity<ResponseMessage> getHolidayList(@RequestParam(required = true) String userId, HttpServletRequest request)
			throws KpoDataException, IOException, JSONException {
		List<HolidayDto> holidayList = dataOperationService.getHolidayList(userId);
		ResponseMessage responseMessage = new ResponseMessage();
		HashMap<String, Object> data = new HashMap<>();
		data.put("data", holidayList);
		responseMessage.setData(data);
		responseMessage.setHttpStatus(HttpStatus.OK);
		responseMessage.setMsg("Holiday List");
		responseMessage.setCode(200);
		return new ResponseEntity<>(responseMessage, HttpStatus.OK);

	}
}
