package com.erp.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;

class HtmlUtilTest {
    @Test void escapesHtmlSpecialCharacters() {
        assertEquals("&lt;tag&gt; &amp; &#39;quoted&#39; &quot;x&quot;", HtmlUtil.escape("<tag> & 'quoted' \"x\""));
    }
}
