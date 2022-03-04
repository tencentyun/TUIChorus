package com.tencent.liteav.demo.chorusimpl;

public interface ChorusConstants {
    String CHORUS_KEY_CMD_VERSION     = "version";
    String CHORUS_KEY_CMD_BUSINESSID  = "businessID";
    String CHORUS_KEY_CMD_PLATFORM    = "platform";
    String CHORUS_KEY_CMD_EXTINFO     = "extInfo";
    String CHORUS_KEY_CMD_DATA        = "data";
    String CHORUS_KEY_CMD_ROOMID      = "room_id";
    String CHORUS_KEY_CMD_CMD         = "cmd";
    String CHORUS_KEY_CMD_SEATNUMBER  = "seat_number";
    String CHORUS_KEY_CMD_INSTRUCTION = "instruction";
    String CHORUS_KEY_CMD_MUSICID     = "music_id";
    String CHORUS_KEY_CMD_CONTENT     = "content";

    int    CHORUS_VALUE_CMD_BASIC_VERSION           = 1;
    int    CHORUS_VALUE_CMD_VERSION                 = 1;
    String CHORUS_VALUE_CMD_BUSINESSID              = "Chorus";
    String CHORUS_VALUE_CMD_PLATFORM                = "Android";
    String CHORUS_VALUE_CMD_PICK                    = "pickSeat";
    String CHORUS_VALUE_CMD_TAKE                    = "takeSeat";
    String CHORUS_VALUE_CMD_INSTRUCTION_MPREPARE    = "m_prepare";
    String CHORUS_VALUE_CMD_INSTRUCTION_MCOMPLETE   = "m_complete";
    String CHORUS_VALUE_CMD_INSTRUCTION_MPLAYMUSIC  = "m_play_music";
    String CHORUS_VALUE_CMD_INSTRUCTION_MSTOP       = "m_stop";
    String CHORUS_VALUE_CMD_INSTRUCTION_MLISTCHANGE = "m_list_change";
    String CHORUS_VALUE_CMD_INSTRUCTION_MPICK       = "m_pick";
    String CHORUS_VALUE_CMD_INSTRUCTION_MDELETE     = "m_delete";
    String CHORUS_VALUE_CMD_INSTRUCTION_MTOP        = "m_top";
    String CHORUS_VALUE_CMD_INSTRUCTION_MNEXT       = "m_next";
    String CHORUS_VALUE_CMD_INSTRUCTION_MGETLIST    = "m_get_list";
    String CHORUS_VALUE_CMD_INSTRUCTION_MDELETEALL  = "m_delete_all";
}
