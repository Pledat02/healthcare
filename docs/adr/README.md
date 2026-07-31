# Architecture Decision Records (ADR)

Ghi lại các quyết định kiến trúc quan trọng + lý do, để người sau hiểu "tại sao" chứ không chỉ "cái gì".

| # | Quyết định | Trạng thái |
|---|---|---|
| [0001](0001-redis-cache-doctor-read-model.md) | Redis cache cho read-model doctor-service (JDK serialization) | Accepted |
| [0002](0002-batch-id-api-strategy.md) | Chiến lược batch API theo danh sách ID (findAllById, cap 100, internal-only) | Accepted |
| [0003](0003-service-to-service-auth.md) | Xác thực service-to-service bằng Keycloak service account | Accepted |

## Quy ước

- Mỗi ADR một file `NNNN-tieu-de-ngan.md`, đánh số tăng dần.
- Không sửa ADR đã Accepted; nếu đổi hướng thì tạo ADR mới và đánh dấu ADR cũ là `Superseded by ADR-NNNN`.
