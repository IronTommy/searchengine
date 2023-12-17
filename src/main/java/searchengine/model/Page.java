package searchengine.model;

import javax.persistence.*;

@Entity
@Table(name = "page")
public class Page {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "site_id", nullable = false)
    private Site site;

    @Column(name = "path", nullable = false, columnDefinition = "TEXT")
    private String path;

    @Column(name = "code", nullable = false)
    private int code;

    @Column(name = "content", nullable = false, columnDefinition = "MEDIUMTEXT")
    private String content;

    public void setSite(Site site) {
        this.site = site;
    }

    public void setPath(String path) {
        this.path = path;
    }
}
