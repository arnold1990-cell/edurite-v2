package com.edurite.bursary.source;

import com.edurite.bursary.dto.BursaryResultDto;
import com.edurite.bursary.dto.BursarySearchRequest;
import java.util.List;

public interface BursarySource {
    List<BursaryResultDto> fetch(BursarySearchRequest request);
    String sourceType();
}

