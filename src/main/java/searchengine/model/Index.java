package searchengine.model;

import javax.persistence.*;

@Entity
@Table(name = "site_index")
public class Index {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long  id;

    @ManyToOne
    @JoinColumn(name = "page_id", referencedColumnName = "id", nullable = false)
    private Page page;

    @ManyToOne
    @JoinColumn(name = "lemma_id", referencedColumnName = "id", nullable = false)
    private Lemma lemma;

    @Column(name = "rank_value")
    private float rankValue;

    @Column(name = "title")
    private String title;

    public Index() {

    }


    public void setPage(Page page) {
        this.page = page;
    }

    public void setRankValue(float rankValue) {
        if (rankValue >= 0.0f && rankValue <= 1.0f) {
            this.rankValue = rankValue;
        } else {
            throw new IllegalArgumentException("Rank value should be between 0.0 and 1.0");
        }
    }

    public Index(Page page, Lemma lemma) {
        this.page = page;
        this.lemma = lemma;
    }

    public void setRank(int rank) {
    }

    public Page getPage() {
        return page;
    }

    public String getTitle() {
        return title;
    }

    public void setLemma(Lemma lemma) {
        this.lemma = lemma;
    }
}
