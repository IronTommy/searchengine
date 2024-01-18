package searchengine.model;

import com.sun.istack.NotNull;
import lombok.*;

import javax.persistence.*;
import java.time.LocalDateTime;

@AllArgsConstructor
@Entity
@Table(name = "site")
@Data
public class Site {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(columnDefinition = "ENUM('INDEXING', 'INDEXED', 'FAILED')", nullable = false)
    private SiteStatus status;

    @NotNull
    @Column(name = "status_time", nullable = false)
    private LocalDateTime statusTime;

    @Column(name = "last_error", columnDefinition = "TEXT")
    private String lastError;

    @NotNull
    @Column(name = "url", nullable = false, length = 255)
    private String url;

    @NotNull
    @Column(name = "name", nullable = false, length = 255)
    private String name;

    public Site() {
    }

    public void initializeName(String defaultName) {
        this.name = (this.name == null || this.name.isEmpty()) ? defaultName : this.name;
    }
}
