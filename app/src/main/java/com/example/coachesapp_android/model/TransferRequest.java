package com.example.coachesapp_android.model;

import java.time.LocalDateTime;

public class TransferRequest {
    private Integer id;
    private Integer playerId;
    private Integer sourceClubId;
    private Integer destinationClubId;
    private TransferStatus status;
    private TransferType transferType;
    private Double transferFee;
    private Double releaseFee;
    private LocalDateTime requestDate;
    private LocalDateTime approvedBySourceDate;
    private LocalDateTime completedDate;
    private String remarks;

    private String playerName;
    private String sourceClubName;
    private String destinationClubName;

    public enum TransferStatus {
        PENDING_APPROVAL("Pending - Awaiting Manager Approval"),
        IN_MARKET("Available in Transfer Market"),
        COMPLETED("Transfer Completed"),
        CANCELLED("Transfer Cancelled");

        private final String displayName;

        TransferStatus(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    public enum TransferType {
        DIRECT_CLUB("Transfer to Specific Club"),
        GENERAL_MARKET("Transfer to General Market");

        private final String displayName;

        TransferType(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    public TransferRequest() {
    }

    public TransferRequest(Integer playerId, Integer sourceClubId, Integer destinationClubId) {
        this.playerId = playerId;
        this.sourceClubId = sourceClubId;
        this.destinationClubId = destinationClubId;
        this.status = TransferStatus.PENDING_APPROVAL;
        this.transferType = destinationClubId == null ? TransferType.GENERAL_MARKET : TransferType.DIRECT_CLUB;
        this.requestDate = LocalDateTime.now();
        this.transferFee = 0.0;
        this.releaseFee = 0.0;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getPlayerId() {
        return playerId;
    }

    public void setPlayerId(Integer playerId) {
        this.playerId = playerId;
    }

    public Integer getSourceClubId() {
        return sourceClubId;
    }

    public void setSourceClubId(Integer sourceClubId) {
        this.sourceClubId = sourceClubId;
    }

    public Integer getDestinationClubId() {
        return destinationClubId;
    }

    public void setDestinationClubId(Integer destinationClubId) {
        this.destinationClubId = destinationClubId;
    }

    public Double getTransferFee() {
        return transferFee;
    }

    public void setTransferFee(Double transferFee) {
        this.transferFee = transferFee;
    }

    public Double getReleaseFee() {
        return releaseFee;
    }

    public void setReleaseFee(Double releaseFee) {
        this.releaseFee = releaseFee;
    }

    public TransferType getTransferType() {
        return transferType;
    }

    public void setTransferType(TransferType transferType) {
        this.transferType = transferType;
    }

    public TransferStatus getStatus() {
        return status;
    }

    public void setStatus(TransferStatus status) {
        this.status = status;
    }

    public LocalDateTime getRequestDate() {
        return requestDate;
    }

    public void setRequestDate(LocalDateTime requestDate) {
        this.requestDate = requestDate;
    }

    public LocalDateTime getApprovedBySourceDate() {
        return approvedBySourceDate;
    }

    public void setApprovedBySourceDate(LocalDateTime approvedBySourceDate) {
        this.approvedBySourceDate = approvedBySourceDate;
    }

    public LocalDateTime getCompletedDate() {
        return completedDate;
    }

    public void setCompletedDate(LocalDateTime completedDate) {
        this.completedDate = completedDate;
    }

    public String getRemarks() {
        return remarks;
    }

    public void setRemarks(String remarks) {
        this.remarks = remarks;
    }

    public String getPlayerName() {
        return playerName;
    }

    public void setPlayerName(String playerName) {
        this.playerName = playerName;
    }

    public String getSourceClubName() {
        return sourceClubName;
    }

    public void setSourceClubName(String sourceClubName) {
        this.sourceClubName = sourceClubName;
    }

    public String getDestinationClubName() {
        return destinationClubName;
    }

    public void setDestinationClubName(String destinationClubName) {
        this.destinationClubName = destinationClubName;
    }

    @Override
    public String toString() {
        return "TransferRequest{" +
                "id=" + id +
                ", playerId=" + playerId +
                ", playerName='" + playerName + '\'' +
                ", sourceClubId=" + sourceClubId +
                ", destinationClubId=" + destinationClubId +
                ", status=" + status +
                ", requestDate=" + requestDate +
                '}';
    }
}
