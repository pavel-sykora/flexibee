package com.adleritech.flexibee.core.api.domain;

import com.adleritech.flexibee.core.api.transformers.Factory;
import org.junit.Test;
import org.simpleframework.xml.Serializer;

import java.io.ByteArrayOutputStream;

import static org.assertj.core.api.Assertions.assertThat;

public class WinstromRequestTest {

    @Test
    public void parseRequestToXml() throws Exception {
        WinstromRequest request = WinstromRequest.builder()
                .issuedInvoice(IssuedInvoice.builder()
                        .company("code:ABCFIRM1#")
                        .documentType(DocumentType.invoice)
                        .withoutItems(true)
                        .sumWithoutVat(1000d)
                        .build()).build();

        ByteArrayOutputStream result = new ByteArrayOutputStream();
        Serializer serializer = Factory.persister();
        serializer.write(request, result);

        String xml = "<winstrom version=\"1.0\">\n" +
                "  <faktura-vydana>\n" +
                "    <typDokl>code:FAKTURA</typDokl>\n" +
                "    <firma>code:ABCFIRM1#</firma>\n" +
                "    <bezPolozek>true</bezPolozek>\n" +
                "    <sumDphZakl>1000.0</sumDphZakl>\n" +
                "  </faktura-vydana>\n" +
                "</winstrom>";
        assertThat(result.toString()).isXmlEqualTo(xml);
    }
}