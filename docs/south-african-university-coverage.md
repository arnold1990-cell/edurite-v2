# South African public university coverage

Registry evidence checked on 2026-10-19 against the [Department of Basic Education university directory](https://www.education.gov.za/FurtherStudies/Universities.aspx). The directory supplied 26 institution names and official website domains. Contact details on that directory can be historical; they are not used as programme evidence.

**Registry coverage: 26 public universities. Live programme coverage: not established by this development run.** Automated tests use mocked websites. No production catalogue backfill or production database access was performed. Existing records are preserved. The authenticated coverage API reports actual database counts, queued pages and source failures after crawling runs.

| Public university | Type | Province(s) | Government-listed website | Programme verification |
| --- | --- | --- | --- | --- |
| Cape Peninsula University of Technology | University Of Technology | Western Cape | https://www.cput.ac.za/ | Information not yet verified. |
| University of Cape Town | Traditional | Western Cape | https://www.uct.ac.za/ | Information not yet verified. |
| Central University of Technology | University Of Technology | Free State | https://www.cut.ac.za/ | Information not yet verified. |
| Durban University of Technology | University Of Technology | KwaZulu-Natal | https://www.dut.ac.za/ | Information not yet verified. |
| University of Fort Hare | Traditional | Eastern Cape | https://www.ufh.ac.za/ | Information not yet verified. |
| University of the Free State | Traditional | Free State | https://www.ufs.ac.za/ | Information not yet verified. |
| University of Johannesburg | Comprehensive | Gauteng | https://www.uj.ac.za/ | Information not yet verified. |
| University of KwaZulu-Natal | Traditional | KwaZulu-Natal | https://www.ukzn.ac.za/ | Information not yet verified. |
| University of Limpopo | Traditional | Limpopo | https://www.ul.ac.za/ | Information not yet verified. |
| Mangosuthu University of Technology | University Of Technology | KwaZulu-Natal | https://www.mut.ac.za/ | Information not yet verified. |
| University of Mpumalanga | Comprehensive | Mpumalanga | https://www.ump.ac.za/ | Information not yet verified. |
| Nelson Mandela University | Comprehensive | Eastern Cape | https://www.nmu.ac.za/ | Information not yet verified. |
| North-West University | Traditional | North West; Gauteng | https://www.nwu.ac.za/ | Information not yet verified. |
| University of Pretoria | Traditional | Gauteng | https://www.up.ac.za/ | Information not yet verified. |
| Rhodes University | Traditional | Eastern Cape | https://www.ru.ac.za/ | Information not yet verified. |
| Sefako Makgatho Health Sciences University | Traditional | Gauteng | https://www.smu.ac.za/ | Information not yet verified. |
| Sol Plaatje University | Comprehensive | Northern Cape | https://www.spu.ac.za/ | Information not yet verified. |
| University of South Africa | Comprehensive | National / distance | https://www.unisa.ac.za/ | Information not yet verified. |
| Stellenbosch University | Traditional | Western Cape | https://www.sun.ac.za/ | Information not yet verified. |
| Tshwane University of Technology | University Of Technology | Gauteng; Limpopo; Mpumalanga | https://www.tut.ac.za/ | Information not yet verified. |
| Vaal University of Technology | University Of Technology | Gauteng | https://www.vut.ac.za/ | Information not yet verified. |
| University of Venda | Comprehensive | Limpopo | https://www.univen.ac.za/ | Information not yet verified. |
| Walter Sisulu University | Comprehensive | Eastern Cape | https://www.wsu.ac.za/ | Information not yet verified. |
| University of the Western Cape | Traditional | Western Cape | https://www.uwc.ac.za/ | Information not yet verified. |
| University of the Witwatersrand | Traditional | Gauteng | https://www.wits.ac.za/ | Information not yet verified. |
| University of Zululand | Comprehensive | KwaZulu-Natal | https://www.unizulu.ac.za/ | Information not yet verified. |

## Unavailable or unverified sources

- All programme catalogues, admission requirements, fees, dates, campus-specific offerings and application portals at the 26 institutions above require individual retrieval/verification. A government-listed website is not proof that every programme or admission rule is current.
- https://www.gov.za/about-sa/education timed out during the direct source check. The accessible DBE directory supplied the institution baseline instead.
- The DHET universities and registers pages returned SharePoint shells through web research. The current private-register PDF could not be established from those shells. Existing private brands are retained as a separate, unverified category and are withheld from verified-source crawling until current linked DHET registration evidence passes verification.
- The DHET PDF found in search is dated 28 February 2026. It is historical evidence, not a claim of current private registration in October 2026: https://www.dhet.gov.za/Registers_DocLib/Register%20%20Private%20Higher%20Education%20Institutions%20%281%29.pdf
- Missing or restricted sources remain explicitly unverified. Robots denials, terms prohibitions, login pages, CAPTCHA and anti-bot challenges are not bypassed.

## Runtime report

`GET /api/ai/university-intelligence/coverage` (also `/api/v1/ai/university-intelligence/coverage`) returns the complete registry including private categories, actual indexed programme counts, successful/queued page counts, last attempted timestamps and failure reasons. The Career Guidance results include a coverage panel that loads this report on demand. Full programme coverage is never inferred from these counts.

Province is institution-level metadata, including multiple provinces or national/distance provision where appropriate. Programme campus evidence is extracted separately and must be checked on the programme source. Registry type metadata does not certify programme accreditation.
