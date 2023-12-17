package searchengine.model;

import javax.persistence.*;

@Entity
@Table(name = "lemma")
public class Lemma {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "site_id", nullable = false)
    private Site site;

    @Column(name = "lemma", nullable = false, length = 255)
    private String lemma;

    @Column(name = "frequency", nullable = false)
    private int frequency;

    // getters and setters
}
