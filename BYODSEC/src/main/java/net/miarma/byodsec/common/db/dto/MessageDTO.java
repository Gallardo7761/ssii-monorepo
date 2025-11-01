package net.miarma.byodsec.common.db.dto;

public class MessageDTO {
    private String fromUserName;
    private String messageText;

    public MessageDTO() {}

    public MessageDTO(String fromUserName, String messageText) {
        this.fromUserName = fromUserName;
        this.messageText = messageText;
    }

	public String getFromUserName() {
		return fromUserName;
	}

	public void setFromUserName(String fromUserName) {
		this.fromUserName = fromUserName;
	}

	public String getMessageText() {
		return messageText;
	}

	public void setMessageText(String messageText) {
		this.messageText = messageText;
	}

    
}