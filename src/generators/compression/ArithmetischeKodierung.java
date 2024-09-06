package generators.compression;

import generators.compression.HelpersArithmetischeKodierung.BigDecimalUtil;

import java.awt.Color;
import java.awt.Font;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Locale;

import algoanim.animalscript.AnimalScript;
import algoanim.primitives.Rect;
import algoanim.primitives.SourceCode;
import algoanim.primitives.StringArray;
import algoanim.primitives.StringMatrix;
import algoanim.primitives.Text;
import algoanim.primitives.generators.Language;
import algoanim.properties.AnimationPropertiesKeys;
import algoanim.properties.ArrayProperties;
import algoanim.properties.MatrixProperties;
import algoanim.properties.RectProperties;
import algoanim.properties.SourceCodeProperties;
import algoanim.properties.TextProperties;
import algoanim.util.Coordinates;
import algoanim.util.Offset;
import generators.framework.Generator;
import generators.framework.GeneratorType;
import generators.framework.ValidatingGenerator;
import generators.framework.properties.AnimationPropertiesContainer;
import interactionsupport.models.MultipleChoiceQuestionModel;

/**
 * @author Egemen Ulutürk <egemen.ulutuerk@stud.tu-darmstadt.de> und
 *         Pascal Schikora <pascal.schikora@stud.tu-darmstadt.de>
 * @version 1.0
 */
public class ArithmetischeKodierung implements ValidatingGenerator {

    /**
     * The concrete language object used for creating output
     */
    private Language lang;
    /**
     * The relative probability from a letter of the en-/decoded word
     */
    private HashMap<Character, BigDecimal> relativeProbabilities;
    /**
     * The alphabet of the en-/decoded word
     */
    private Character[] givenAlphabet;
    /**
     * Length of the en-/decoded word
     */
    private BigDecimal length;
    /**
     * The word to en-/decode
     */
    private String inputWord;
    /**
     * Entropy of the input word
     */
    private double entropy;
    /**
     * Properties of the SourceCode
     */
    private SourceCodeProperties scProps;
    /**
     * Properties of the input word array
     */
    private ArrayProperties inputArrayProps;
    /**
     * Properties of the interval boxes
     */
    private RectProperties intervalProps;
    /**
     * Properties of the text used in the animation
     */
    private TextProperties textProps;
    /**
     * Properties of the matrix used for the probabilites
     */
    private MatrixProperties probMatrix;
    /**
     * The title of the animation
     */
    private Text header;
    /**
     * The box around the title
     */
    private Rect headerRect;
    /**
     * The input array
     */
    private StringArray inputArr;
    /**
     * The source code for the animation
     */
    private SourceCode sourceCode;
    /**
     * The probability array description
     */
    private StringMatrix probArrayDesc;
    /**
     *
     */
    private final int intervalLength = 800;

    /**
     * Short description of the algorithm
     */
    private static final String DESCRIPTION = "Der Algorithmus für die arithmetische Kodierung ist eine Form der Entropiekodierung."
            + System.lineSeparator()
            + "Er wird zur verlustfreien Datenkompression verwendet und erzielt Kompressionsraten, welche sehr nahe am theoretischen Limit der Entropie liegen."
            + System.lineSeparator()
            + "Der Algorithmus basiert auf der Verteilung der verwendeten Zeichen und der schrittweisen Aufteilung von Intervallen.";

    /**
     * The algorithm as pseudocode
     */
    private static final String PSEUDO_CODE = "1. Bestimme Alphabet der Eingabe mit der relativen Häufigkeit der Zeichen" // 1
            + System.lineSeparator() + "2. Initialisiere das Starintervall [0,1)" + System.lineSeparator()
            + "3*. Erstelle innerhalb des Hauptintervalls Subintervalle mit den relativen Häufigkeiten der Zeichen"
            + System.lineSeparator() + "4*. Bestimme Subintervall des aktuellen Zeichens" + System.lineSeparator()
            + "5*. Setze Hauptintervall gleich dem bestimmten Subintervall" + System.lineSeparator()
            + "6. Wenn die Eingabe vollständig iteriert wurde, gebe den kleinsten Wert des aktuellen Intervalls aus. Ansonsten springe zu Punkt 3";

    /**
     * Default constructor
     *
     * @param lang the conrete language object used for creating output
     */
    public ArithmetischeKodierung(Language lang) {
        super();
        this.lang = lang;
    }

    /**
     * Default constructor
     */
    public ArithmetischeKodierung() {
        super();
    }

    @Override
    public void init() {
        // Store the new language object
        this.lang = new AnimalScript(getAlgorithmName(), getAnimationAuthor(), 1500, 600);
        // This initializes the step mode. Each pair of subsequent steps has to
        // be divdided by a call of lang.nextStep();
        this.lang.setStepMode(true);

        this.lang.setInteractionType(Language.INTERACTION_TYPE_AVINTERACTION);

        this.relativeProbabilities = new HashMap<>();
    }

    @Override
    public boolean validateInput(AnimationPropertiesContainer animationPropertiesContainer, Hashtable<String, Object> primitives) throws IllegalArgumentException {
        String input = (String) primitives.get("input"); // load input
        String pattern = "\\s*";
        // check if input is valid
        if(input == null || input.matches(pattern)) throw new IllegalArgumentException("Der Input darf nicht leer sein!"); // invalid input
        return true; // valid input
    }

    @Override
    public String generate(AnimationPropertiesContainer props, Hashtable<String, Object> primitives) {
        // Load user props
        this.inputArrayProps = (ArrayProperties) props.getPropertiesByName("inputArray");
        this.scProps = (SourceCodeProperties) props.getPropertiesByName("sourceCode");
        this.intervalProps = (RectProperties) props.getPropertiesByName("interval");
        this.textProps = (TextProperties) props.getPropertiesByName("text");
        this.probMatrix = (MatrixProperties) props.getPropertiesByName("probMatrix");

        // Load user input
        String input = (String) primitives.get("input");
        this.inputWord = input;
        this.length = new BigDecimal(input.length()); // set length of the word

        // create title
        TextProperties headerProps = new TextProperties();
        headerProps.set(AnimationPropertiesKeys.FONT_PROPERTY, new Font(Font.SANS_SERIF, Font.BOLD, 24));
        this.header = lang.newText(new Coordinates(20, 30), "Arithmetische Kodierung", "header", null, headerProps);

        // create box around title
        RectProperties rectProps = new RectProperties();
        rectProps.set(AnimationPropertiesKeys.FILLED_PROPERTY, true);
        rectProps.set(AnimationPropertiesKeys.FILL_PROPERTY, Color.WHITE);
        rectProps.set(AnimationPropertiesKeys.DEPTH_PROPERTY, 2);
        this.headerRect = lang.newRect(new Offset(-5, -5, "header", AnimalScript.DIRECTION_NW), new Offset(5, 5, "header", AnimalScript.DIRECTION_SE), "headerRect", null, rectProps);

        // setup start page with description
        this.lang.nextStep();
        this.lang.newText(new Coordinates(10, 100), "Die arithmetische Kodierung dient zur verlustfreien Datenkompression.", "description1", null, this.textProps);
        this.lang.newText(new Offset(0, 3, "description1", AnimalScript.DIRECTION_SW), "Sie erzielt Kompressionsraten sehr nahe am theoretischen Limit der Entropie.", "description2", null, this.textProps);
        this.lang.newText(new Offset(0, 3, "description2", AnimalScript.DIRECTION_SW), "Hierbei werden die Quellinformationen nicht in einzelne Komponente aufgeteilt, .", "description3", null, this.textProps);
        this.lang.newText(new Offset(0, 3, "description3", AnimalScript.DIRECTION_SW), "sondern die gesamte Quellinformation (oder zumindest längere Teilbereiche) als eine rationale Zahl dargestellt", "description4", null, this.textProps);
        this.lang.newText(new Offset(0, 3, "description4", AnimalScript.DIRECTION_SW), "Grundsätzlich ist die arithmetische Kodierung rechenintensiver als herkömmliche Verfahren,", "description5", null, this.textProps);
        this.lang.newText(new Offset(0, 3, "description5", AnimalScript.DIRECTION_SW), "welche Codewörter mit einer Anzahl ganzzahliger Bits bilden.", "description6", null, this.textProps);
        this.lang.newText(new Offset(0, 3, "description6", AnimalScript.DIRECTION_SW), "Theoretisch kann das Verfahren mit unendlich genauen reelen Zahlen arbeiten.", "description7", null, this.textProps);
        this.lang.newText(new Offset(0, 3, "description7", AnimalScript.DIRECTION_SW), "Allerdings müssen bei der Implementierung leider endlich genaue Zahlentypen verwendet werden.", "description8", null, this.textProps);
        this.lang.newText(new Offset(0, 3, "description8", AnimalScript.DIRECTION_SW), "Dies führt dazu, dass durch Rundungen häufig nicht ein optimales Ergebnis erreicht wird.", "description9", null, this.textProps);

        // setup source code on new page
        this.lang.nextStep("Einleitung");

        // create blank page
        this.lang.hideAllPrimitives();
        this.header.show();
        this.headerRect.show();
        // add source code
        this.sourceCode = lang.newSourceCode(new Coordinates(10, 100), "sourceCode", null, this.scProps);
        this.sourceCode.addCodeLine("1. Bestimme Alphabet der Eingabe mit der relativen Häufigkeit der Zeichen.", null, 0, null); // 0
        this.sourceCode.addCodeLine("2. Initialisiere das Starintervall [0,1).", null, 0, null); // 1
        this.sourceCode.addCodeLine("3. Erstelle innerhalb des Hauptintervalls Subintervalle mit den relativen Häufigkeiten der Zeichen.", null, 0, null); // 2
        this.sourceCode.addCodeLine("4. Bestimme Subintervall des aktuellen Zeichens. Wenn das aktuelle Zeichen das letzte ist, springe zu Punkt 6.", null, 1, null); // 3
        this.sourceCode.addCodeLine("5. Setze Hauptintervall gleich dem bestimmten Subintervall.", null, 1, null); // 4
        this.sourceCode.addCodeLine("6. Wenn die Eingabe vollständig iteriert wurde, gebe den kleinsten Wert des aktuellen Intervalls aus.", null, 0, null); // 5

        // show input word as array
        this.lang.nextStep();
        String[] inputArr = input.split("(?!^)");

        this.inputArr = lang.newStringArray(new Offset(0, 30, "sourceCode", AnimalScript.DIRECTION_SW), inputArr, "inputWord", null, this.inputArrayProps);

        this.calculateProbabilites();
        this.encode();

        this.lang.finalizeGeneration();
        return this.lang.toString();
    }

    /**
     * Calculates the probability of a char within the given word
     */
    private void calculateProbabilites() {
        // mark first line of source code
        sourceCode.highlight(0);
        // setup probability array
        ArrayList<ArrayList<String>> probs = new ArrayList<>();
        probs.add(new ArrayList<>());
        probs.get(0).add("Zeichen");
        probs.get(0).add("abs. Häufigkeit");
        probs.get(0).add("rel. Häufigkeit");
        this.probArrayDesc = lang.newStringMatrix(new Offset(0, 30, "inputWord", AnimalScript.DIRECTION_SW), probs.stream().map(u -> u.toArray(new String[0])).toArray(String[][]::new), "probabilityDescription", null, probMatrix);

        // Calculate normalized one
        BigDecimal normalized = BigDecimal.ONE.divide(this.length, 20, RoundingMode.HALF_UP);
        int counter = 1;
        HashMap<Character, Integer> count = new HashMap<>();
        HashMap<Character, Integer> row = new HashMap<>();
        char[] arr = this.inputWord.toCharArray();

        // iterate over input string
        for(int i = 0; i < arr.length; i++){
            char c = arr[i];
            if(i == 0) this.lang.nextStep("Bestimme relative Häufigkeiten");
            else this.lang.nextStep();

            if(i > 0) this.inputArr.unhighlightCell(i-1, null, null);
            this.inputArr.highlightCell(i, null, null);

            if(this.relativeProbabilities.containsKey(c)) { // character already exists in list
                count.put(c, count.get(c)+1);
                this.relativeProbabilities.put(c, this.relativeProbabilities.get(c).add(normalized));

                probs.get(row.get(c)).set(1, count.get(c) + "");
                probs.get(row.get(c)).set(2, this.relativeProbabilities.get(c).toString());
                this.probArrayDesc.put(row.get(c), 1, count.get(c) + "", null, null);
                this.probArrayDesc.put(row.get(c), 2, this.relativeProbabilities.get(c).toString(), null, null);

            } else { // no entry for that character in the list
                this.probArrayDesc.hide();

                row.put(c, counter++);
                ArrayList<String> iter = new ArrayList<>();
                iter.add(String.valueOf(c));
                iter.add("1");
                iter.add(normalized.toString());
                probs.add(iter);

                String[][] stringArray = probs.stream().map(u -> u.toArray(new String[0])).toArray(String[][]::new);
                this.probArrayDesc = this.lang.newStringMatrix(new Offset(0, 30, "inputWord", AnimalScript.DIRECTION_SW), stringArray, "probabilityDescription", null, probMatrix);

                count.put(c, 1);
                this.relativeProbabilities.put(c, normalized);
            }
        }

        // Saves the given alphabet
        this.givenAlphabet = this.relativeProbabilities.keySet().stream().sorted().toArray(Character[]::new);
    }

    /**
     * Encodes the given string
     */
    private void encode() {
        // next step in animation and mark appropriate code line
        lang.nextStep();
        sourceCode.unhighlight(0);
        sourceCode.highlight(1);
        this.inputArr.unhighlightCell(this.inputWord.length()-1, null, null);

        // show empty rectangle with initalized values
        Rect emptyRect = lang.newRect(new Offset(100, 0, "probabilityDescription", AnimalScript.DIRECTION_NE), new Offset(this.intervalLength + 100, 60, "probabilityDescription", AnimalScript.DIRECTION_NE), "emptyRect", null, intervalProps);
        emptyRect.changeColor(AnimalScript.COLORCHANGE_FILLCOLOR, Color.LIGHT_GRAY, null, null);
        Text textZero = lang.newText(new Offset(-2, 5, "emptyRect", AnimalScript.DIRECTION_SW), "0", "textZero", null, this.textProps);
        Text textOne = lang.newText(new Offset(-2, 5, "emptyRect", AnimalScript.DIRECTION_SE), "1", "textOne", null, this.textProps);

        // next setp (remove initialization rect) and move highlight next sc line
        lang.nextStep("Beginn des Algorithmus");
        emptyRect.hide();
        textZero.hide();
        textOne.hide();
        sourceCode.unhighlight(1);
        sourceCode.highlight(2);

        // initialize interval display
        HashMap<Character, Rect> rects = new HashMap<>();
        Text[] intervalTexts = new Text[this.givenAlphabet.length + 1];

        // create a rect per character
        for(int i = 0; i < this.givenAlphabet.length; i++) {
            char ch = this.givenAlphabet[i];
            // calculate length of rect
            BigDecimal bdCurrLength = BigDecimal.valueOf(intervalLength).multiply(this.relativeProbabilities.get(ch));
            int currLength = bdCurrLength.intValue();
            Rect rect;

            if(i > 0) { // all other rects offset to the previos rect
                String previousRectName = "rect" + this.givenAlphabet[i-1];
                // create rect
                rect = lang.newRect(new Offset(0, 0, previousRectName, AnimalScript.DIRECTION_NE), new Offset(currLength, 0, previousRectName, AnimalScript.DIRECTION_SE), "rect" + ch, null, intervalProps);
                // create interval text
                intervalTexts[i] = lang.newText(new Offset(-3, 5, "rect" + ch, AnimalScript.DIRECTION_SW), "","intervalBorder_" + i, null, textProps);
            } else { // first rect offset to probabilityDescription
                // create first rect
                rect = lang.newRect(new Offset(100, 0, "probabilityDescription", AnimalScript.DIRECTION_NE), new Offset(currLength + 100, 60, "probabilityDescription", AnimalScript.DIRECTION_NE), "rect" + ch, null, intervalProps);
                // create first interval text
                intervalTexts[i] = lang.newText(new Offset(-3, 5, "rect" + ch, AnimalScript.DIRECTION_SW), "0", "intervalBorder_" + i, null, textProps);
            }
            // set character as text within the rect
            lang.newText(new Offset((currLength/2)-3, 25, "rect" + ch, AnimalScript.DIRECTION_NW), ""+ch, "text"+ ch, null, textProps);
            rects.put(ch, rect); // save in hashmap
        }
        // set last interval number (1)
        intervalTexts[intervalTexts.length - 1] = lang.newText(new Offset(-3, 65, "rect" + this.givenAlphabet[this.givenAlphabet.length - 1], AnimalScript.DIRECTION_NE), "1","intervalBorder_" + this.givenAlphabet.length, null, textProps);

        // start of real algorithm

        BigDecimal currentIntervalStart = BigDecimal.ZERO;
        BigDecimal currentIntervalEnd = BigDecimal.ONE;

        BigDecimal nextIntervalStart = currentIntervalStart;
        BigDecimal nextIntervalEnd = currentIntervalEnd;

        BigDecimal letterStart = currentIntervalStart;
        BigDecimal letterEnd = currentIntervalEnd;

        // loop over every char in word to encode
        int selectedInterval = -1;
        for(int i = 0; i < this.inputWord.length(); i++) {
            char currentLetter = this.inputWord.toCharArray()[i];
            if(i > 0) {
                lang.nextStep();
                sourceCode.unhighlight(3);
                sourceCode.highlight(4);
            }

            // highlight current letter in input array and unhiglight the last one (if there was one already)
            if(i > 0 ) inputArr.unhighlightCell(i-1, null, null);
            inputArr.highlightCell(i, null, null);

            // reset old selected chararter highlighting from the rect
            if(selectedInterval != -1) rects.get(this.givenAlphabet[selectedInterval]).changeColor(AnimalScript.COLORCHANGE_FILLCOLOR, Color.WHITE, null, null);


            if(!currentIntervalStart.equals(BigDecimal.ZERO)) intervalTexts[0].setText(currentIntervalStart.setScale(5, RoundingMode.HALF_UP).toString(), null, null);
            if(!currentIntervalStart.equals(BigDecimal.ONE)) intervalTexts[intervalTexts.length - 1].setText(currentIntervalEnd.setScale(5, RoundingMode.HALF_UP).toString(), null, null);

            int pos = 1;
            // hide old labels
            for(int j = 1; j < intervalTexts.length - 1; j++) {
                intervalTexts[j].hide();
            }

            lang.nextStep();

            sourceCode.unhighlight(4);
            sourceCode.highlight(2);

            String correctAnswer = String.valueOf(currentLetter);

            for(int j = 1; j < intervalTexts.length - 1; j++) {
                intervalTexts[j].show();
            }

            for(int j = 0; j < this.givenAlphabet.length; j++) {
                char alphabet = this.givenAlphabet[j];

                // calculate end of letter interval
                letterEnd = currentIntervalEnd.subtract(currentIntervalStart).multiply(this.relativeProbabilities.get(alphabet)).add(letterStart);
                //letterEnd = (intervalEnd - intervalStart) * this.relativeProbabilities.get(alphabet) + letterStart;

                // check if the current letter is the needed letter and break loop to stop calculating the other intervals
                if(currentLetter == alphabet) {
                    nextIntervalStart = letterStart;
                    nextIntervalEnd = letterEnd;
                    selectedInterval = j; // set which interval got selected (important for rect highlighting)
                }
                // assign new values to variables to calculate the next interval
                letterStart = letterEnd;
                letterEnd = currentIntervalEnd;

                intervalTexts[pos++].setText(letterStart.setScale(5, RoundingMode.HALF_UP).toString(), null, null);
            }

            MultipleChoiceQuestionModel mcq = new MultipleChoiceQuestionModel("multipleChoiceQuestion" + i);
            mcq.setPrompt("Auf welches Subintervall wird das Hauptintervall gesetzt? Auf das Interval von...");
            for (int k = 0; k < givenAlphabet.length; k++) {
                if (currentLetter == givenAlphabet[k]) {
                    mcq.addAnswer(String.valueOf(givenAlphabet[k]), 1, "Korrekt! " + givenAlphabet[k] + " ist das nächste Intervall.");
                } else {
                    mcq.addAnswer(givenAlphabet[k] + "", 0, "Falsch! Die richtige Antwort lautet " + correctAnswer);
                }
            }
            mcq.setGroupID("Intervalbestimmung");
            lang.addMCQuestion(mcq);

            lang.nextStep();

            currentIntervalStart = nextIntervalStart;
            currentIntervalEnd = nextIntervalEnd;
            letterStart = currentIntervalStart;

            // highlight selected interval/rect
            rects.get(this.givenAlphabet[selectedInterval]).changeColor(AnimalScript.COLORCHANGE_FILLCOLOR, Color.ORANGE, null, null);
            sourceCode.unhighlight(2);
            sourceCode.highlight(3);
        }

        // end of algorithm

        // unhighlight everything
        lang.nextStep();

        sourceCode.unhighlight(3);
        sourceCode.highlight(5);

        inputArr.unhighlightCell(inputWord.length()-1, null, null); // unhighlight last char in inputArray

        // show information about interval
        lang.newText(new Offset(10, 60, "rect" + this.givenAlphabet[0], AnimalScript.DIRECTION_SW), "Der Algorithmus ist nun fertig mit der Berechnung. Das genaue Interval ist wie folgt (aus Gründen der Übersichtlichkeit hier auf zehn Stellen gekürzt): [" + nextIntervalStart.setScale(10, RoundingMode.HALF_UP) + ", " + nextIntervalEnd.setScale(10, RoundingMode.HALF_UP) + ").", "infoAfterInterval", null, this.textProps);
        lang.newText(new Offset(0, 3, "infoAfterInterval", AnimalScript.DIRECTION_SW), "Nun kann ein beliebiger Wert innerhalb des Intervals gewählt werden, um ein Endergebnis zu erhalten.", "infoAfterInterval1", null, this.textProps);
        lang.newText(new Offset(0, 3, "infoAfterInterval1", AnimalScript.DIRECTION_SW), "Dabei sollte darauf geachtet werden, dass der gewählte Wert aus möglichst wenig Bits besteht.", "infoAfterInterval2", null, this.textProps);
        lang.newText(new Offset(0, 3, "infoAfterInterval2", AnimalScript.DIRECTION_SW), "Ein Richtwert dafür ist die Entropie. Sie gibt den theoretischen Bits-Verbrauch eines Wortes an.", "infoAfterInterval3", null, this.textProps);
        lang.newText(new Offset(0, 3, "infoAfterInterval3", AnimalScript.DIRECTION_SW), "Sie kann über folgende vereinfachte Formel bestimmt werden: ", "infoAfterInterval4", null, this.textProps);
        lang.newText(new Offset(10, 3, "infoAfterInterval4", AnimalScript.DIRECTION_SW), "Entropie = ∑ n_i * I(z_i)", "infoAfterInterval5", null, this.textProps);
        lang.newText(new Offset(-10, 3, "infoAfterInterval5", AnimalScript.DIRECTION_SW), "n_i ist die absolute Häufigkeit eines Buchstaben und I(z_i) ist die optimale Bitzahl eines Buchstaben mit der relativen Häufigkeit z_i.", "infoAfterInterval6", null, this.textProps);
        lang.newText(new Offset(0, 3, "infoAfterInterval6", AnimalScript.DIRECTION_SW),"Die optimale Bitzahl eines Buchstaben wird über die Formel -log2(z_i) bestimmt.", "infoAfterInterval7", null, this.textProps);
        // calculate entropy
        this.entropy = this.calculateEntropy().doubleValue();
        // more information
        lang.newText(new Offset(0, 3, "infoAfterInterval7", AnimalScript.DIRECTION_SW), "Für das Input-Wort " + this.inputWord + " liegt damit der theoretische Informationsgehalt bei " + this.entropy + " Bits.", "infoAfterInterval8", null, this.textProps);
        lang.newText(new Offset(0, 3, "infoAfterInterval8", AnimalScript.DIRECTION_SW), "Als nächstes bestimmen wir einen Wert, der möglichst wenig Bits für unser Interval benötigt und so möglichst nah an den theoretischen Bit-Wert kommt.", "infoAfterInterval9", null, this.textProps);
        // get shortes representation from interval
        BigDecimal result = leastDigits(nextIntervalStart, nextIntervalEnd);
        // more text
        lang.newText(new Offset(0, 3, "infoAfterInterval9", AnimalScript.DIRECTION_SW), "Ein möglicher Wert wäre " + result.toString() + ".", "infoAfterInterval10", null, this.textProps);

        // calculate count of used bits in result with least digits
        byte[] bytes = result.unscaledValue().toByteArray(); // convert to bytes
        int bitCount = 0;
        boolean found = false;
        for(int i = bytes.length - 1; i >= 0; i--){ // go through array backwards in order to not forget 0 bytes within the number
            byte currentByte = bytes[i];
            if(!found && currentByte == 0x0) continue; // check if last byte is 0 and skip it
            if(!found && currentByte > 0x0) { // check if it is bigger than 0
                int relevantBits = 0;
                while(currentByte > 0x0) { // left shift the byte until it is 0 and count how many bits are therefor relevant
                    relevantBits++;
                    currentByte <<= currentByte;
                }
                found = true; // found a byte that is relevant
                bitCount += relevantBits; // add counted relevant bits
                continue; // skip addition of 8
            }
            bitCount += 8; // since every byte is now relevant just add 8 bits
        }
        // more information
        lang.newText(new Offset(0, 3, "infoAfterInterval10", AnimalScript.DIRECTION_SW), "Demnach hat unser berechneter Wert einen Informationsgehalt von " + bitCount + " Bits.", "infoAfterInterval11", null, this.textProps);
        lang.nextStep("Zusammenfassung");
    }


    /**
     * Returns for a given interval a number with the least needed digits within the interval
     * @param intervalStart the start of the interval (needs to be the lower than the end)
     * @param intervalEnd the end of the interval (needs to be bigger than the start)
     */
    private BigDecimal leastDigits(BigDecimal intervalStart, BigDecimal intervalEnd) {
        // convert interval to String
        char[] start = intervalStart.toString().toCharArray();
        char[] end = intervalEnd.toString().toCharArray();

        // determine length of shortest number
        int minLength = Math.min(start.length, end.length);
        // Set initial position of rounding to the minimum length in order to calculate that properly if no other rounding position was found
        int position = minLength;
        for(int i = 0; i < minLength; i++) {
            // Check if the the two numbers are different at the current position
            if(start[i] != end[i]){
                // the value can not be equal to the upper interval since it would round inproperly
                position = end[i] - start[i] == 1 ? i + 1 : i;
                break; // break at the first different value
            }
        }
        // round the interval at the right position
        return this.roundUp(intervalStart, position - 1);
    }

    /**
     * Rounds up a given BigDecimal to a given length
     * @param bd the BigDecimal to round up
     * @param places how long the rounded BigDecimal should be
     */
    private BigDecimal roundUp(BigDecimal bd, int places) {
        if (places < 0) throw new IllegalArgumentException();
        // rounds the up the number
        bd = bd.setScale(places, RoundingMode.UP);
        return bd;
    }

    /**
     * Calculates the entropy of the input word (formula: ∑ n_i * -log2(z_i))
     */
    private BigDecimal calculateEntropy() {
        BigDecimal sum = BigDecimal.ZERO;
        for(Character c : this.relativeProbabilities.keySet()){
            BigDecimal ln = BigDecimalUtil.ln(this.relativeProbabilities.get(c), 10);
            BigDecimal base2 = ln.divide(BigDecimalUtil.ln(BigDecimal.valueOf(2), 10), RoundingMode.HALF_UP);
            sum = sum.add(this.relativeProbabilities.get(c).multiply(this.length).multiply(base2.negate()));
        }
        return sum;
    }

    /**
     * Getter for the relative probabilities
     */
    public HashMap<Character, BigDecimal> getRelativeProbabilities() {
        return relativeProbabilities;
    }

    /**
     * Gibt den Namen des Algorithmus zurück
     */
    @Override
    public String getName() {
        return "Arithmetische Kodierung [DE]";
    }

    /**
     * Gibt den Namen des Algorithmus zurück
     */
    @Override
    public String getAlgorithmName() {
        return "Arithmetische Kodierung [DE]";
    }

    /**
     * Gibt die Namen der Authoren zurück
     */
    @Override
    public String getAnimationAuthor() {
        return "Pascal Schikora und Egemen Ulutürk";
    }

    /**
     * Gibt die Sprache der Implementierung des Algorithmus zurück
     */
    @Override
    public Locale getContentLocale() {
        return Locale.GERMAN;
    }

    /**
     * Gibt die File Extension der generierten Datei zurück
     */
    @Override
    public String getFileExtension() {
        return Generator.ANIMALSCRIPT_FORMAT_EXTENSION;
    }

    /**
     * Gibt den Typ des Algorithmus/Generators zurück
     */
    @Override
    public GeneratorType getGeneratorType() {
        return new GeneratorType(GeneratorType.GENERATOR_TYPE_COMPRESSION);
    }

    /**
     * Gibt die Art des umgesetzten Algorithmus zurück
     */
    @Override
    public String getOutputLanguage() {
        return Generator.PSEUDO_CODE_OUTPUT;
    }

    /**
     * Gibt Beispiel-Code zurück
     */
    @Override
    public String getCodeExample() {
        return PSEUDO_CODE;
    }

    /**
     * Gibt die Beschreibung des Algorithmus zurück
     */
    @Override
    public String getDescription() {
        return DESCRIPTION;
    }
}
