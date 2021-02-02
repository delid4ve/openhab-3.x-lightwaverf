package org.openhab.binding.lightwaverf.internal.smart.dto.websocket;

import java.util.ArrayList;
import java.util.List;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class InitialStatus {

    @SerializedName("version")
    @Expose
    private Integer version;
    @SerializedName("senderId")
    @Expose
    private String senderId;
    @SerializedName("direction")
    @Expose
    private String direction;
    @SerializedName("items")
    @Expose
    private List<Item> items = new ArrayList<Item>();
    @SerializedName("class")
    @Expose
    private String _class;
    @SerializedName("operation")
    @Expose
    private String operation;
    @SerializedName("transactionId")
    @Expose
    private Integer transactionId;

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public String getSenderId() {
        return senderId;
    }

    public void setSenderId(String senderId) {
        this.senderId = senderId;
    }

    public String getDirection() {
        return direction;
    }

    public void setDirection(String direction) {
        this.direction = direction;
    }

    public List<Item> getItems() {
        return items;
    }

    public void setItems(List<Item> items) {
        this.items = items;
    }

    public String getClass_() {
        return _class;
    }

    public void setClass_(String _class) {
        this._class = _class;
    }

    public String getOperation() {
        return operation;
    }

    public void setOperation(String operation) {
        this.operation = operation;
    }

    public Integer getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(Integer transactionId) {
        this.transactionId = transactionId;
    }

    public class Item {

        @SerializedName("itemId")
        @Expose
        private String itemId;
        @SerializedName("success")
        @Expose
        private Boolean success;
        @SerializedName("payload")
        @Expose
        private Payload payload;

        public String getItemId() {
            return itemId;
        }

        public void setItemId(String itemId) {
            this.itemId = itemId;
        }

        public Boolean getSuccess() {
            return success;
        }

        public void setSuccess(Boolean success) {
            this.success = success;
        }

        public Payload getPayload() {
            return payload;
        }

        public void setPayload(Payload payload) {
            this.payload = payload;
        }

        public class Payload {

            @SerializedName("workerUniqueId")
            @Expose
            private String workerUniqueId;
            @SerializedName("token")
            @Expose
            private String token;

            @SerializedName("handlerId")
            @Expose
            private String handlerId;
            @SerializedName("featureId")
            @Expose
            private String featureId;
            @SerializedName("deviceId")
            @Expose
            private Integer deviceId;
            @SerializedName("type")
            @Expose
            private String type;
            @SerializedName("channel")
            @Expose
            private Integer channel;
            @SerializedName("writable")
            @Expose
            private Boolean writable;
            @SerializedName("stateless")
            @Expose
            private Boolean stateless;
            @SerializedName("virtual")
            @Expose
            private Boolean virtual;
            @SerializedName("value")
            @Expose
            private Long value;
            @SerializedName("status")
            @Expose
            private String status;
            @SerializedName("clientDeviceId")
            @Expose
            private String clientDeviceId;
            @SerializedName("_feature")
            @Expose
            private Feature feature;

            public String getWorkerUniqueId() {
                return workerUniqueId;
            }

            public void setWorkerUniqueId(String workerUniqueId) {
                this.workerUniqueId = workerUniqueId;
            }

            public String getHandlerId() {
                return handlerId;
            }

            public void setHandlerId(String handlerId) {
                this.handlerId = handlerId;
            }

            public String getToken() {
                return token;
            }

            public void setToken(String token) {
                this.token = token;
            }

            public String getFeatureId() {
                return featureId;
            }

            public void setFeatureId(String featureId) {
                this.featureId = featureId;
            }

            public Integer getDeviceId() {
                return deviceId;
            }

            public void setDeviceId(Integer deviceId) {
                this.deviceId = deviceId;
            }

            public String getType() {
                return type;
            }

            public void setType(String type) {
                this.type = type;
            }

            public Integer getChannel() {
                return channel;
            }

            public void setChannel(Integer channel) {
                this.channel = channel;
            }

            public Boolean getWritable() {
                return writable;
            }

            public void setWritable(Boolean writable) {
                this.writable = writable;
            }

            public Boolean getStateless() {
                return stateless;
            }

            public void setStateless(Boolean stateless) {
                this.stateless = stateless;
            }

            public Boolean getVirtual() {
                return virtual;
            }

            public void setVirtual(Boolean virtual) {
                this.virtual = virtual;
            }

            public Long getValue() {
                return value;
            }

            public void setValue(Long value) {
                this.value = value;
            }

            public String getStatus() {
                return status;
            }

            public void setStatus(String status) {
                this.status = status;
            }

            public String getClientDeviceId() {
                return this.clientDeviceId;
            }

            public void setClientDeviceId(String clientDeviceId) {
                this.clientDeviceId = clientDeviceId;
            }

            public Feature getFeature() {
                return feature;
            }

            public void setFeature(Feature feature) {
                this.feature = feature;
            }

            public class Feature {

                @SerializedName("deviceId")
                @Expose
                private String deviceId;
                @SerializedName("productCode")
                @Expose
                private String productCode;
                @SerializedName("featureId")
                @Expose
                private String featureId;
                @SerializedName("featureType")
                @Expose
                private String featureType;

                public String getDeviceId() {
                    return deviceId;
                }

                public void setDeviceId(String deviceId) {
                    this.deviceId = deviceId;
                }

                public String getProductCode() {
                    return productCode;
                }

                public void setProductCode(String productCode) {
                    this.productCode = productCode;
                }

                public String getFeatureId() {
                    return featureId;
                }

                public void setFeatureId(String featureId) {
                    this.featureId = featureId;
                }

                public String getFeatureType() {
                    return featureType;
                }

                public void setFeatureType(String featureType) {
                    this.featureType = featureType;
                }
            }
        }
    }
}
