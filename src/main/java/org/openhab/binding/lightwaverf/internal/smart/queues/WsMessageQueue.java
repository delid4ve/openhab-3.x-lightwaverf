package org.openhab.binding.lightwaverf.internal.smart.queues;

import org.openhab.binding.lightwaverf.internal.smart.dto.websocket.InitialStatus;

public class WsMessageQueue {

    private Integer transactionid;
    private InitialStatus initialStatus;

    public InitialStatus getInitialStatus() {
        return this.initialStatus;
    }

    public void setInitialStatus(InitialStatus initialStatus) {
        this.initialStatus = initialStatus;
    }

    public Integer getTransactionid() {
        return this.transactionid;
    }

    public void setTransactionid(Integer transactionid) {
        this.transactionid = transactionid;
    }
}
