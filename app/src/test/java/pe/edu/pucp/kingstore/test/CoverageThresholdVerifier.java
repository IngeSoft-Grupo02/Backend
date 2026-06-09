package pe.edu.pucp.kingstore.test;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.DecimalFormat;

public final class CoverageThresholdVerifier {

    private CoverageThresholdVerifier() {
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            throw new IllegalArgumentException("Uso: CoverageThresholdVerifier <jacoco.xml> <minimumRatio>");
        }

        Path reportPath = Path.of(args[0]);
        double minimum = Double.parseDouble(args[1]);

        if (!Files.isRegularFile(reportPath)) {
            throw new IllegalStateException("No existe el reporte agregado de JaCoCo: " + reportPath);
        }

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);

        Document document = factory.newDocumentBuilder().parse(reportPath.toFile());
        NodeList counters = document.getDocumentElement().getElementsByTagName("counter");

        Element instructionCounter = null;
        for (int i = 0; i < counters.getLength(); i++) {
            Element counter = (Element) counters.item(i);
            if ("INSTRUCTION".equals(counter.getAttribute("type"))) {
                instructionCounter = counter;
            }
        }

        if (instructionCounter == null) {
            throw new IllegalStateException("El reporte de JaCoCo no contiene contador INSTRUCTION");
        }

        int covered = Integer.parseInt(instructionCounter.getAttribute("covered"));
        int missed = Integer.parseInt(instructionCounter.getAttribute("missed"));
        double ratio = covered + missed == 0 ? 0.0 : (double) covered / (covered + missed);

        DecimalFormat percent = new DecimalFormat("0.00%");
        System.out.println("JaCoCo aggregate instruction coverage: " + percent.format(ratio)
                + " (minimum " + percent.format(minimum) + ")");

        if (ratio < minimum) {
            throw new IllegalStateException("Cobertura agregada insuficiente: "
                    + percent.format(ratio) + " < " + percent.format(minimum));
        }
    }
}
