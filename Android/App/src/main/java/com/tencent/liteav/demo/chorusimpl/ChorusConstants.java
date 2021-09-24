package com.tencent.liteav.demo.chorusimpl;

public interface ChorusConstants {
     String Chorus_KEY_CMD_VERSION     = "version";
     String Chorus_KEY_CMD_BUSINESSID  = "businessID";
     String Chorus_KEY_CMD_PLATFORM    = "platform";
     String Chorus_KEY_CMD_EXTINFO     = "extInfo";
     String Chorus_KEY_CMD_DATA        = "data";
     String Chorus_KEY_CMD_ROOMID      = "room_id";
     String Chorus_KEY_CMD_CMD         = "cmd";
     String Chorus_KEY_CMD_SEATNUMBER  = "seat_number";
     String Chorus_KEY_CMD_INSTRUCTION = "instruction";
     String Chorus_KEY_CMD_MUSICID     = "music_id";
     String Chorus_KEY_CMD_CONTENT     = "content";

     int    Chorus_VALUE_CMD_BASIC_VERSION           = 1;
     int    Chorus_VALUE_CMD_VERSION                 = 1;
     String Chorus_VALUE_CMD_BUSINESSID              = "Chorus";
     String Chorus_VALUE_CMD_PLATFORM                = "Android";
     String Chorus_VALUE_CMD_PICK                    = "pickSeat";
     String Chorus_VALUE_CMD_TAKE                    = "takeSeat";
     String Chorus_VALUE_CMD_INSTRUCTION_MPREPARE    = "m_prepare";
     String Chorus_VALUE_CMD_INSTRUCTION_MCOMPLETE   = "m_complete";
     String Chorus_VALUE_CMD_INSTRUCTION_MPLAYMUSIC  = "m_play_music";
     String Chorus_VALUE_CMD_INSTRUCTION_MSTOP       = "m_stop";
     String Chorus_VALUE_CMD_INSTRUCTION_MLISTCHANGE = "m_list_change";
     String Chorus_VALUE_CMD_INSTRUCTION_MPICK       = "m_pick";
     String Chorus_VALUE_CMD_INSTRUCTION_MDELETE     = "m_delete";
     String Chorus_VALUE_CMD_INSTRUCTION_MTOP        = "m_top";
     String Chorus_VALUE_CMD_INSTRUCTION_MNEXT       = "m_next";
     String Chorus_VALUE_CMD_INSTRUCTION_MGETLIST    = "m_get_list";
     String Chorus_VALUE_CMD_INSTRUCTION_MDELETEALL  = "m_delete_all";
}
