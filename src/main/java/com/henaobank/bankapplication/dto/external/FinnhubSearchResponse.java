package com.henaobank.bankapplication.dto.external;

import java.util.List;

public record FinnhubSearchResponse(Integer count, List<FinnhubSearchItem> result) {
}
