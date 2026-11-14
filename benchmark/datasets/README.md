# benchmark/datasets

| 文件 | 数据规模 | 用途 |
| --- | --- | --- |
| `generate-accounts.sql` | 10,000 账户（PG） | 多账户随机转账基准 |
| `generate-accounts-mysql.sql` | 10,000 账户（MySQL 8.4） | 同上 |

数据分布必须明确记录：热点账户占比（建议 30% 流量打 1 个账户）、账户余额分布、
金额分布、时间跨度。否则 TPS/P99 数字不可复现、不可比较。