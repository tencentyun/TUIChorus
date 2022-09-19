package com.tencent.liteav.demo.chorusimpl;

import com.google.gson.annotations.SerializedName;

public class ChorusJsonData {

    private int    version;
    private String businessID;
    private String platform;
    private Data   data;

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public String getBusinessID() {
        return businessID;
    }

    public void setBusinessID(String businessID) {
        this.businessID = businessID;
    }

    public String getPlatform() {
        return platform;
    }

    public void setPlatform(String platform) {
        this.platform = platform;
    }

    public Data getData() {
        return data;
    }

    public void setData(Data data) {
        this.data = data;
    }

    public static class Data {

        @SerializedName("room_id")
        private String roomId;

        @SerializedName("instruction")
        private String instruction;

        @SerializedName("content")
        private String content;

        @SerializedName("music_id")
        private String musicId;

        public String getRoomId() {
            return roomId;
        }

        public void setRoomId(String roomId) {
            this.roomId = roomId;
        }

        public String getInstruction() {
            return instruction;
        }

        public void setInstruction(String instruction) {
            this.instruction = instruction;
        }

        public String getContent() {
            return content;
        }

        public void setContent(String content) {
            this.content = content;
        }

        public String getMusicId() {
            return musicId;
        }

        public void setMusicId(String musicId) {
            this.musicId = musicId;
        }
    }
}
