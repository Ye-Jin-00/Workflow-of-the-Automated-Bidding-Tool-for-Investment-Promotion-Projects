package com.sincit.bid.ai;

import com.sincit.bid.domain.AiDraft;
import com.sincit.bid.domain.BidProject;

/**
 * AI 生成能力抽象。本地模板与真实大模型实现同一接口，便于降级与替换。
 */
public interface AiProvider {

    String name();

    AiDraft generate(BidProject project, String emphasis, String tone);
}
