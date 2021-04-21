/**
 * Copyright (c) 2010-2020 Contributors to the openHAB project
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.openhab.binding.lightwaverf.internal.dto;

import java.io.Serializable;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

/**
 *
 * @author David Murton - Initial contribution
 */
public class LightwaverfConnectResponse implements Serializable {

    @SerializedName("trans")
    @Expose
    private Integer trans;
    @SerializedName("mac")
    @Expose
    private String mac;
    @SerializedName("time")
    @Expose
    private Integer time;
    @SerializedName("pkt")
    @Expose
    private String pkt;
    @SerializedName("fn")
    @Expose
    private String fn;
    @SerializedName("source_id")
    @Expose
    private String sourceId;
    @SerializedName("source")
    @Expose
    private String source;
    @SerializedName("type")
    @Expose
    private String type;
    @SerializedName("payload")
    @Expose
    private String payload;

    @SerializedName("cmd")
    @Expose
    private String cmd;
    @SerializedName("lat")
    @Expose
    private Double lat;
    @SerializedName("long")
    @Expose
    private Double _long;
    @SerializedName("offset")
    @Expose
    private Integer offset;
    @SerializedName("prod")
    @Expose
    private String prod;
    @SerializedName("fw")
    @Expose
    private String fw;
    @SerializedName("uptime")
    @Expose
    private Long uptime;
    @SerializedName("timezone")
    @Expose
    private Integer timezone;
    @SerializedName("tmrs")
    @Expose
    private Integer tmrs;
    @SerializedName("evns")
    @Expose
    private Integer _evns;
    @SerializedName("run")
    @Expose
    private Integer run;
    @SerializedName("macs")
    @Expose
    private Integer macs;
    @SerializedName("ip")
    @Expose
    private String ip;
    @SerializedName("devs")
    @Expose
    private Integer devs;
    @SerializedName("room")
    @Expose
    private Integer room;
    @SerializedName("dev")
    @Expose
    private Integer dev;
    @SerializedName("pairType")
    @Expose
    private String pairType;
    @SerializedName("msg")
    @Expose
    private String msg;
    @SerializedName("class")
    @Expose
    private String _class;
    @SerializedName("serial")
    @Expose
    private String serial;
    @SerializedName("param")
    @Expose
    private Integer param;

    private final static long serialVersionUID = 43195024342242149L;

    public Integer getTrans() {
        return trans;
    }

    public void setTrans(Integer trans) {
        this.trans = trans;
    }

    public String getMac() {
        return mac;
    }

    public void setMac(String mac) {
        this.mac = mac;
    }

    public Integer getTime() {
        return time;
    }

    public void setTime(Integer time) {
        this.time = time;
    }

    public String getPkt() {
        return pkt;
    }

    public void setPkt(String pkt) {
        this.pkt = pkt;
    }

    public String getFn() {
        return fn;
    }

    public void setFn(String fn) {
        this.fn = fn;
    }

    public String getSourceId() {
        return sourceId;
    }

    public void setSourceId(String sourceId) {
        this.sourceId = sourceId;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getPayload() {
        return payload;
    }

    public void setPayload(String payload) {
        this.payload = payload;
    }

    public String getCmd() {
        return this.cmd;
    }

    public void setCmd(String cmd) {
        this.cmd = cmd;
    }

    public Double getLat() {
        return this.lat;
    }

    public void setLat(Double lat) {
        this.lat = lat;
    }

    public Double get_long() {
        return this._long;
    }

    public void set_long(Double _long) {
        this._long = _long;
    }

    public Integer getOffset() {
        return this.offset;
    }

    public void setOffset(Integer offset) {
        this.offset = offset;
    }

    public String getProd() {
        return this.prod;
    }

    public void setProd(String prod) {
        this.prod = prod;
    }

    public String getFw() {
        return this.fw;
    }

    public void setFw(String fw) {
        this.fw = fw;
    }

    public Long getUptime() {
        return this.uptime;
    }

    public void setUptime(Long uptime) {
        this.uptime = uptime;
    }

    public Integer getTimezone() {
        return this.timezone;
    }

    public void setTimezone(Integer timezone) {
        this.timezone = timezone;
    }

    public Integer getTmrs() {
        return this.tmrs;
    }

    public void setTmrs(Integer tmrs) {
        this.tmrs = tmrs;
    }

    public Integer get_evns() {
        return this._evns;
    }

    public void set_evns(Integer _evns) {
        this._evns = _evns;
    }

    public Integer getRun() {
        return this.run;
    }

    public void setRun(Integer run) {
        this.run = run;
    }

    public Integer getMacs() {
        return this.macs;
    }

    public void setMacs(Integer macs) {
        this.macs = macs;
    }

    public String getIp() {
        return this.ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public Integer getDevs() {
        return this.devs;
    }

    public void setDevs(Integer devs) {
        this.devs = devs;
    }

    public Integer getRoom() {
        return this.room;
    }

    public void setRoom(Integer room) {
        this.room = room;
    }

    public Integer getDev() {
        return this.dev;
    }

    public void setDev(Integer dev) {
        this.dev = dev;
    }

    public String getPairType() {
        return this.pairType;
    }

    public void setPairType(String pairType) {
        this.pairType = pairType;
    }

    public String getSerial() {
        return this.serial;
    }

    public void setSerial(String serial) {
        this.serial = serial;
    }

    public String getMsg() {
        return this.msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public String get_class() {
        return this._class;
    }

    public void set_class(String _class) {
        this._class = _class;
    }

    public Integer getParam() {
        return this.param;
    }

    public void setParam(Integer param) {
        this.param = param;
    }
}
