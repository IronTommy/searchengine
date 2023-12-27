package searchengine.model;

import javax.persistence.*;

@Entity
@Table(name = "site_index")
public class Index {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "page_id", referencedColumnName = "id", nullable = false)
    private Page page;

    @ManyToOne
    @JoinColumn(name = "lemma_id", referencedColumnName = "id", nullable = false)
    private Lemma lemma;

    @Column(name = "rank_value")
    private float rankValue;

    public void setPage(Page page) {
        this.page = page;
    }

    public void setRankValue(float rankValue) {
        this.rankValue = rankValue;
    }

    public void setLemma(Lemma lemma) {
        this.lemma = lemma;
    }
}
