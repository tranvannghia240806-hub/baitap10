# Bài tập JWT – Spring Boot 3 / Security 6

| Thư mục | Nội dung |
|---|---|
| `jwt-jjwt/`   | **Phần 1** – làm đúng bài giảng (10 bước) với thư viện **jjwt 0.12.6** |
| `jwt-nimbus/` | **Phần 2** – cùng ứng dụng, thay jjwt bằng **Nimbus JOSE + JWT 9.47** |

Chỉ 3 chỗ khác nhau giữa hai project: `pom.xml`, `services/JwtService.java`, `exception/GlobalExceptionHandler.java`
(+ 2 exception mới `InvalidTokenException`, `TokenExpiredException`). Filter, Controller, Security config giữ nguyên.

## Chạy

Yêu cầu: JDK 17+, Maven.

```bash
# Cách 1 - SQL Server (tạo sẵn DB trong SSMS: CREATE DATABASE jwt_springboot3;)
#   sửa user/password trong src/main/resources/application.properties
mvn spring-boot:run

# Cách 2 - chạy nhanh bằng H2 in-memory, không cần SQL Server
mvn spring-boot:run -Dspring-boot.run.profiles=h2
```
Server: http://localhost:8005 (hai project dùng chung cổng 8005 – chạy từng cái một).

## Tạo database bằng file SQL (SQL Server)

Trong thư mục `database/` có `jwt_springboot3.sql`. Mở file này trong SSMS, bấm **Execute (F5)** một lần:
tạo database `jwt_springboot3`, bảng `users` và tài khoản mẫu **`trungnh@hcmute.edu.vn` / `123456`**.
Sau đó chạy ứng dụng rồi đăng nhập luôn, không cần gọi `/auth/signup`. Chạy lại file nhiều lần vẫn an toàn.

Nếu không chạy file SQL, ứng dụng vẫn tự tạo bảng (`ddl-auto=update`) nhưng bạn phải tự tạo database rỗng
và đăng ký tài khoản qua `/auth/signup`.

## Test (Bước 9) – Postman hoặc curl

```bash
# 1. Đăng ký
curl -X POST localhost:8005/auth/signup -H 'Content-Type: application/json' \
     -d '{"email":"trungnh@hcmute.edu.vn","password":"123456","fullName":"Nguyễn Hữu Trung"}'

# 2. Đăng nhập -> nhận token
curl -X POST localhost:8005/auth/login -H 'Content-Type: application/json' \
     -d '{"email":"trungnh@hcmute.edu.vn","password":"123456"}'
TOKEN=<dán token>

# 3. Không token -> 401
curl -i localhost:8005/users/me
# 4. Có token -> 200
curl -i localhost:8005/users/me -H "Authorization: Bearer $TOKEN"
curl -i localhost:8005/users/   -H "Authorization: Bearer $TOKEN"
# 5. Token sai định dạng -> 401 ; token bị sửa 1 ký tự -> 401
curl -i localhost:8005/users/me -H "Authorization: Bearer dfdfdfdfd"
# 6. Sai mật khẩu -> 401
curl -i -X POST localhost:8005/auth/login -H 'Content-Type: application/json' \
     -d '{"email":"trungnh@hcmute.edu.vn","password":"sai"}'
```
Giao diện Ajax (Bước 10): mở http://localhost:8005/login → đăng nhập → tự chuyển tới `/user/profile`.
Token hết hạn: đặt `security.jwt.expiration-time=5000` (5 giây), đăng nhập, đợi rồi gọi `/users/me` → 401 "The JWT token has expired".

## Đối chiếu jjwt ↔ Nimbus

| Việc | jjwt 0.12.6 | Nimbus JOSE + JWT |
|---|---|---|
| Dependency | `jjwt-api` + `jjwt-impl` + `jjwt-jackson` | 1 artifact `nimbus-jose-jwt` |
| Khoá HS256 | `Keys.hmacShaKeyFor(bytes)` → `SecretKey` | `byte[]` đưa thẳng vào `MACSigner` / `MACVerifier` (≥ 32 byte) |
| Tạo claims | `Jwts.builder().subject().issuedAt().expiration()` | `new JWTClaimsSet.Builder().subject().issueTime().expirationTime().build()` |
| Ký | `.signWith(key, Jwts.SIG.HS256).compact()` | `new SignedJWT(header, claims)` → `.sign(new MACSigner(key))` → `.serialize()` |
| Đọc + verify | `Jwts.parser().verifyWith(key).build().parseSignedClaims(t)` | `SignedJWT.parse(t)` → `.verify(new MACVerifier(key))` → `.getJWTClaimsSet()` |
| Kiểm tra hạn | **tự động** ném `ExpiredJwtException` | **phải tự kiểm** `exp` (đã làm trong `extractAllClaims`) |
| Kiểm tra `alg` | jjwt tự từ chối `none` | **phải tự kiểm** `alg == HS256` (đã làm) |
| Lỗi | `SignatureException`, `ExpiredJwtException`, `MalformedJwtException` (RuntimeException) | `ParseException`, `JOSEException` (checked) → bọc thành `InvalidTokenException` |

## Khác biệt so với slide (cố ý sửa)
1. Slide hard-code hạn token 30 giờ trong `buildToken`; bản này dùng `security.jwt.expiration-time` (1 giờ).
2. `User.images` để `nullable=false` nhưng `signup` không set → lỗi lưu DB; bản này gán ảnh mặc định.
3. `password` (dù đã băm BCrypt) không còn trả ra JSON (`@JsonIgnore`).
4. Bảng lỗi ghi Signature/Expired = 401 nhưng code slide trả 403 → dùng 401. Token rác (`dfdfdf`) trả 401 thay vì 500 (slide 31).
5. Thiếu/không có token → 401 (Security 6 mặc định 403) nhờ `HttpStatusEntryPoint`.
6. `mainjs.js`: chỉ gọi `/users/me` ở trang profile (slide gọi cả ở trang login → luôn báo lỗi), và sửa biến `e` chưa khai báo.
7. Bật `.cors(...)` để `CorsConfigurationSource` trong slide thực sự có tác dụng.

## Chạy bằng VS Code + JDK 25

1. **Cài**: JDK 25, Maven 3.9+ (hoặc dùng `./mvnw` nếu bạn tự thêm wrapper), VS Code.
   Kiểm tra trong Terminal: `java -version` (phải 25) và `mvn -v` (dòng "Java version" phải là 25).
2. **Extension** (VS Code → Extensions): *Extension Pack for Java* và *Spring Boot Extension Pack*.
   Lombok đã có sẵn trong pack Java, không cần cài thêm.
3. **Mở project**: `File → Open Folder…` chọn **`jwt-jjwt`** (hoặc `jwt-nimbus`) — mở đúng thư mục chứa `pom.xml`, không mở thư mục cha.
   Đợi góc dưới bên phải hết "Importing Maven project".
4. **Trỏ VS Code tới JDK 25** nếu nó dùng JDK khác: `Ctrl+Shift+P` → *Java: Configure Java Runtime* → đặt JDK 25.
   Nếu thấy báo lỗi đỏ ở `@Data`/getter: `Ctrl+Shift+P` → *Java: Clean Java Language Server Workspace* → Restart.
5. **Chạy không cần SQL Server** (khuyên dùng để test nhanh) — Terminal trong VS Code:
   ```bash
   mvn spring-boot:run -Dspring-boot.run.profiles=h2
   ```
   Thấy `Started JwtSpringboot3Application` và `Tomcat started on port 8005` là xong.
   *Hoặc bấm nút ▶ Run ở dòng `main` của `JwtSpringboot3Application.java`*; khi đó muốn dùng H2 thì
   mở `.vscode/launch.json` và thêm `"args": "--spring.profiles.active=h2"`.
6. **Chạy với SQL Server**: tạo DB `CREATE DATABASE jwt_springboot3;` trong SSMS, sửa username/password trong
   `application.properties` (mặc định `sa` / `123456`), rồi `mvn spring-boot:run`.
7. **Test**: mở http://localhost:8005/login (đăng ký trước bằng curl/Postman ở mục Test phía trên), hoặc cài extension
   *REST Client* / *Thunder Client* để gọi API ngay trong VS Code.
8. **Debug**: đặt breakpoint (vd trong `JwtService`), bấm *Debug* trên dòng `main`, rồi gọi API.
9. **Dừng server**: `Ctrl+C` trong Terminal (hoặc nút ■). Hai project cùng cổng 8005 nên chỉ chạy một cái mỗi lần.

Lỗi thường gặp
- `Port 8005 already in use` → đang chạy project kia/lần chạy trước; tắt đi hoặc đổi `server.port`.
- `Login failed for user` / `The TCP/IP connection to the host has failed` → SQL Server chưa chạy, sai mật khẩu, chưa tạo DB, hoặc chưa bật TCP/IP port 1433 (SQL Server Configuration Manager) và SQL Authentication; dùng profile `h2` để thử nhanh.
- `java: invalid source release` / báo sai phiên bản → Maven đang dùng JDK khác 25: xem `mvn -v`, đặt `JAVA_HOME` về JDK 25.
