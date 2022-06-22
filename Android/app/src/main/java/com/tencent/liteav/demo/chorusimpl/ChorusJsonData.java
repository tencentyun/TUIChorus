package com.tencent.liteav.demo.chorusimpl;


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

        private String room_id;
        private String instruction;
        private String content;
        private String music_id;

        public String getRoomId() {
            return room_id;
        }

        public void setRoomId(String room_id) {
            this.room_id = room_id;
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
            return music_id;
        }

        public void setMusicId(String music_id) {
            this.music_id = music_id;
        }
    }
}
