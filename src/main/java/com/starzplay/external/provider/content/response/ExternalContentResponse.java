package com.starzplay.external.provider.content.response;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

/**
 * 
 * @author Chandra Sekhar Babu A
 *
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "$xmlns",
    "startIndex",
    "itemsPerPage",
    "entryCount",
    "title",
    "entries"
})
public class ExternalContentResponse {

    @JsonProperty("$xmlns")
    private com.starzplay.external.provider.content.response.Xmlns $xmlns;
    @JsonProperty("startIndex")
    private Double startIndex;
    @JsonProperty("itemsPerPage")
    private Double itemsPerPage;
    @JsonProperty("entryCount")
    private Double entryCount;
    @JsonProperty("title")
    private String title;
    @JsonProperty("entries")
    private List<Entry> entries = null;
    @JsonIgnore
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();

    @JsonProperty("$xmlns")
    public com.starzplay.external.provider.content.response.Xmlns get$xmlns() {
        return $xmlns;
    }

    @JsonProperty("$xmlns")
    public void set$xmlns(com.starzplay.external.provider.content.response.Xmlns $xmlns) {
        this.$xmlns = $xmlns;
    }

    @JsonProperty("startIndex")
    public Double getStartIndex() {
        return startIndex;
    }

    @JsonProperty("startIndex")
    public void setStartIndex(Double startIndex) {
        this.startIndex = startIndex;
    }

    @JsonProperty("itemsPerPage")
    public Double getItemsPerPage() {
        return itemsPerPage;
    }

    @JsonProperty("itemsPerPage")
    public void setItemsPerPage(Double itemsPerPage) {
        this.itemsPerPage = itemsPerPage;
    }

    @JsonProperty("entryCount")
    public Double getEntryCount() {
        return entryCount;
    }

    @JsonProperty("entryCount")
    public void setEntryCount(Double entryCount) {
        this.entryCount = entryCount;
    }

    @JsonProperty("title")
    public String getTitle() {
        return title;
    }

    @JsonProperty("title")
    public void setTitle(String title) {
        this.title = title;
    }

    @JsonProperty("entries")
    public List<Entry> getEntries() {
        return entries;
    }

    @JsonProperty("entries")
    public void setEntries(List<Entry> entries) {
        this.entries = entries;
    }

    @JsonAnyGetter
    public Map<String, Object> getAdditionalProperties() {
        return this.additionalProperties;
    }

    @JsonAnySetter
    public void setAdditionalProperty(String name, Object value) {
        this.additionalProperties.put(name, value);
    }

}
