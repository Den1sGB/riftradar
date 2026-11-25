package com.riftradar.riot;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.riftradar.common.ApiException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class RiotRegionTest {

    @Test
    void mapsKnownPlatformsCaseInsensitively() {
        assertThat(RiotRegion.fromPlatform("br1")).isEqualTo(RiotRegion.BR1);
        assertThat(RiotRegion.fromPlatform("EUW1").regionalHost()).isEqualTo("europe");
        assertThat(RiotRegion.fromPlatform("kr").regionalHost()).isEqualTo("asia");
    }

    @Test
    void rejectsMaliciousOrUnsupportedPlatform() {
        assertThatThrownBy(() -> RiotRegion.fromPlatform("evil.com#"))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getStatus())
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }
}
