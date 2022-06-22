package com.tencent.liteav.tuichorus.ui.gift;

import java.util.List;

public interface OnGiftListQueryCallback {
    /**
     * 查询成功 响应结果
     *
     * @param giftInfoList
     */
    void onGiftListQuerySuccess(List<GiftData> giftInfoList);

    /**
     * 查询失败
     */
    void onGiftListQueryFailed(String errorMsg);
}
