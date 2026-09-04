# rag_multi_format_app.py - Quick Test Questions

Sample Q&A against the default `data/rag-multi/` corpus (shipping-policy.md,
warranty-info.html, returns-policy.txt). Use to sanity-check retrieval after
changes - not ingested as a source doc (lives outside `data/rag-multi/`).

Run:
```
python examples/09_rag/rag_multi_format_app.py --provider gemini --interactive
```

| # | Question | Expected answer |
|---|----------|------------------|
| 1 | What is the warranty on the ABC laptop? | 24-month limited warranty; accidental/water damage not covered. |
| 2 | What is the warranty on the XYZ smartphone? | 12-month limited warranty covering manufacturing defects. |
| 3 | Can I return an item after 40 days? | No - returns are only accepted within 30 days of delivery. |
| 4 | Do opened cables and cases qualify for a refund? | No - opened software and consumable accessories (cables, cases) are final sale. |
| 5 | How long does domestic shipping take on a $60 order? | Free, 5-7 business days (order qualifies since it's over $50). |
| 6 | How much extra for 2-3 day shipping? | $12 flat fee for expedited shipping. |
| 7 | How long do international orders take, and who pays customs? | 10-15 business days; customs fees are charged by the destination country. |
| 8 | How do I file a warranty claim? | Contact support with the order number and a description of the defect. |
| 9 | How long until a refund posts after a return is received? | 5-10 business days, to the original payment method. |
| 10 | I bought the ABC laptop 40 days ago and it's defective - can I get a refund or repair? | No refund (past the 30-day return window), but it's still covered by the 24-month warranty - file a warranty claim. |
