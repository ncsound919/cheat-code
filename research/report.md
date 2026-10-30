# Formula bench - measured

Source: `nba_2008-2026.csv`  |  games used: **19,810** of 24,440

## F1 - pricing
- no-vig of -110/-110 = **0.500000**, hold = **4.7619%**
- mean hold across the book = **3.745%** (median 3.759%, p95 4.708%)
- rows with negative hold (arbitrage): 0

## F2 - market calibration (the benchmark)
- home win rate: 0.5866
- Brier (de-vig): **0.202329**  vs 0.25 coin flip  |  with vig: 0.202962
- log loss (de-vig): **0.589246**  |  with vig: 0.591227
- ECE (10 bins): **0.005610**

| bin | n | mean p | observed | gap |
|---|---|---|---|---|
| 0.0-0.1 | 63 | 0.0769 | 0.0952 | 0.0183 |
| 0.1-0.2 | 522 | 0.1617 | 0.1705 | 0.0088 |
| 0.2-0.3 | 1305 | 0.2549 | 0.2452 | 0.0097 |
| 0.3-0.4 | 2203 | 0.3529 | 0.3650 | 0.0121 |
| 0.4-0.5 | 2429 | 0.4472 | 0.4450 | 0.0021 |
| 0.5-0.6 | 3059 | 0.5524 | 0.5381 | 0.0143 |
| 0.6-0.7 | 3637 | 0.6495 | 0.6486 | 0.0009 |
| 0.7-0.8 | 3304 | 0.7474 | 0.7488 | 0.0014 |
| 0.8-0.9 | 2523 | 0.8463 | 0.8442 | 0.0021 |
| 0.9-1.0 | 765 | 0.9257 | 0.9307 | 0.0050 |

## F3 - Kelly
- closed form matched the numeric growth-rate maximum in every synthetic case: **True** (max error 2.0e-06)
- at the vigged price, fraction of rows with a positive Kelly stake: **0.0000** (max f* -0.0003)
- reading: the de-vigged probability is not an edge against the same book's price; that is the expected result, and it is why PropsLab needs a *model* probability, not a de-vigged market one.
