package generators;

import animal.main.Animal;
import generators.graphics.OptischeTriangulation;

public class Main {

    public static void main(String[] args) {
        //ArithmetischeKodierung ak = new ArithmetischeKodierung();
        // Encode
        //String word = "AAABAAAC";
        //BigDecimal encoded = ak.arithmethicCoder(word);
        //System.out.println(encoded.toString());
        
        // Decode
        //ArithmetischeKodierung adk = new ArithmetischeKodierung();
        //String decoded = adk.arithmeticDecoder(ak.getRelativeProbabilities(), BigDecimal.valueOf(word.length()), encoded);
        //System.out.println(decoded);

        // Animal.startGeneratorWindow(new ArithmetischeKodierung());
        Animal.startGeneratorWindow(new OptischeTriangulation());
    }
}