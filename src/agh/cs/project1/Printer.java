package agh.cs.project1;

import java.io.IOException;
import java.util.LinkedList;
import java.util.Map;

public class Printer {
    private Map<Elements, String> options;
    private boolean constitution;

    public Printer(Map<Elements, String> options) {
        this.options = options;
    }

    public void print() throws IOException, IllegalArgumentException {
        ArgumentsVerifier verifier = new ArgumentsVerifier(options);
        constitution = verifier.verifyArgs();

        Preprocessor cleaner = new Preprocessor(options.get(Elements.File));
        cleaner.clean(!constitution);
        Organizer organizer = new Organizer(cleaner.getList());
        Document doc = organizer.organize(constitution);

        if (options.get(Elements.TOC) != null && options.get(Elements.Section) != null) {
            this.writeSectionTOC(doc, options.get(Elements.Section));
        }

        else if (options.get(Elements.TOC) != null) {
            this.writeTOC(doc);
        }

        else if (constitution && options.get(Elements.Chapter) != null) {
            this.writeChapterConst(doc, options.get(Elements.Chapter));
        }

        else if (!constitution && options.get(Elements.Section) != null && options.get(Elements.Chapter) != null) {
            this.writeChapterUokik(doc, options.get(Elements.Section), options.get(Elements.Chapter));
        }

        else if (!constitution && options.get(Elements.Section) != null) {
            this.writeSection(doc, options.get(Elements.Section));
        }

        else if (options.get(Elements.ArticleRange) != null) {
            this.writeArticleRange(doc, options.get(Elements.ArticleRange));
        }

        else if (options.get(Elements.Article) != null) {
            String artInd = options.get(Elements.Article);
            DocElement art = doc.getArticle(artInd);
            if (art == null)
                throw new IllegalArgumentException("Artykuł o indeksie " + artInd + " nie występuje.");

            if (options.get(Elements.Point) == null && options.get(Elements.Paragraph) == null) {
                this.writeArticle(doc, options.get(Elements.Article));
            }
            else if (options.get(Elements.Paragraph) != null) {

                String parInd = options.get(Elements.Paragraph);
                DocElement par = art.getChild(parInd);
                if (par == null || (par.getContent().charAt(1) != '.' && par.getContent().charAt(2) != '.'))
                    throw new IllegalArgumentException("Ustęp o indeksie " + parInd + " nie występuje w artykule " + artInd);

                if (options.get(Elements.Point) != null) {
                    String poiInd = options.get(Elements.Point);
                    DocElement point = par.getChild(poiInd);
                    if (point == null)
                        throw new IllegalArgumentException("Punkt o indeksie " + poiInd + " nie występuje w ustępie " + parInd);

                    if (options.get(Elements.Letter) != null) {
                        // art ust pkt lit
                        String letInd = options.get(Elements.Letter);
                        DocElement letter = point.getChild(letInd);
                        if (letter == null)
                            throw new IllegalArgumentException("Literał o indeksie " + letInd + " nie występuje w punkcie " + poiInd);
                        letter.deepPrinter(letter);
                    }
                    else {
                        // art ust pkt
                        point.deepPrinter(point);
                    }

                }

                else {
                    // art ust
                    par.deepPrinter(par);
                }

            }
            else if (options.get(Elements.Point) != null) {
                String poiInd = options.get(Elements.Point);
                //System.out.println(poiInd);
                DocElement point = art.getChild(poiInd);
                if (point == null || (point.getContent().charAt(1) != ')' &&
                        point.getContent().charAt(2) != ')' && point.getContent().charAt(3) != ')'))
                    throw new IllegalArgumentException("Punkt o indeksie " + poiInd + " nie występuje w artykule " + artInd);

                if (options.get(Elements.Letter) != null) {
                    // art pkt lit
                    String letInd = options.get(Elements.Letter);
                    DocElement letter = point.getChild(letInd);
                    if (letter == null)
                        throw new IllegalArgumentException("Literał o indeksie " + letInd + " nie występuje w punkcie " + poiInd);
                    letter.deepPrinter(letter);
                }
                else {
                    // art pkt
                    point.deepPrinter(point);
                }
            }
        }
    }

    private void writeSectionTOC(Document doc, String id) throws  IllegalArgumentException {
        if(((Uokik) doc).getSection(id) != null) {
            ((Uokik) doc).getSection(id).printContent();
            LinkedList<DocElement> chapters = ((Uokik) doc).getSection(id).getChildren();
            if (chapters.get(0).getContent().startsWith("Rozdział")) {
                for (int i = 0; i < chapters.size(); i++) {
                    chapters.get(i).printContent();
                }
                //System.out.println("...........................................");
            }
        }
        else {
            throw new IllegalArgumentException("Dział o numerze: " + id + " nie występuje.");
        }
    }

    private void writeSection(Document doc, String id) throws  IllegalArgumentException {
        if(((Uokik) doc).getSection(id) != null) {
            DocElement section = ((Uokik) doc).getSection(id);
            section.deepPrinter(section);
        }
        else {
            throw new IllegalArgumentException("Dział o numerze: " + id + " nie występuje.");
        }
    }

    private void writeTOC(Document doc) {
        if(constitution) {
            LinkedList<DocElement> chapters = ((Constitution) doc).getChapters();
            for (int i = 0; i < chapters.size(); i++) {
                chapters.get(i).printContent();
            }
        }
        else {
            LinkedList<DocElement> sections = ((Uokik) doc).getSections();
            for (int i = 0; i < sections.size(); i++) {
                writeSectionTOC(doc, sections.get(i).getId());
            }
        }
    }

    private void writeArticle (Document doc, String index) {
        if (doc.getArticle(index) != null) {
            DocElement article = doc.getArticle(index);
            article.deepPrinter(article);
        }
        else throw new IllegalArgumentException("Artykuł o numerze: " + index + " nie występuje.");
    }

    private void writeArticleRange (Document doc, String range) {
        LinkedList<DocElement> articles = doc.getArticles();
        int space = range.indexOf(' ');
        String start = range.substring(0, space);
        String end = range.substring(space+1, range.length());

        if (!doc.checkRange(start, end))
            throw new IllegalArgumentException("Podano niepoprawny zakres artykułów.");

        int i = 0;
        while (i < articles.size() && !articles.get(i).getId().equals(start)) i++;

        while (i < articles.size() && !articles.get(i).getId().equals(end)) {
            articles.get(i).deepPrinter(articles.get(i));
            i++;
        }
        if (i < articles.size() && articles.get(i).getId().equals(end)) {
            articles.get(i).deepPrinter(articles.get(i));
        }
    }

    private void writeChapterConst (Document doc, String index) {
        if (((Constitution) doc).getChapter(index) != null) {
            DocElement chapter = ((Constitution) doc).getChapter(index);
            chapter.deepPrinter(chapter);
        }
        else throw new IllegalArgumentException("Rozdział o numerze: " + index + " nie występuje.");
    }

    private void writeChapterUokik (Document doc, String section, String chapter) {
        if (((Uokik) doc).getChapter(section, chapter) != null) {
            DocElement chap = ((Uokik) doc).getChapter(section, chapter);
            chap.deepPrinter(chap);
        }
        else throw new IllegalArgumentException("Rozdział o podanych parametrach nie występuje.");
    }
}
