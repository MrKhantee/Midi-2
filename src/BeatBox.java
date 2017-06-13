import javax.sound.midi.*;
import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.util.ArrayList;

public class BeatBox {
    private JPanel mainPanel;
    private ArrayList<JCheckBox> checkBoxList;
    private Sequencer sequencer;
    private Sequence sequence;
    private Track track;
    private JFrame theFrame;

    private String[] instrumentNames = { "Bass Drum", "Closed Hi-Hat", "Open Hi-Hat",
        "Acoustic Share", "Crash Cymbal", "Hand Clap", "High Tom", "Hi Bongo",
        "Maracas", "Whistle", "Low Conga", "Cowbell", "Vibraslap", "Low-mid Tom",
            "High Agogo", "Open Hi Conga"};
    private int[] instruments = {35,42,46,38,49,39,50,60,70,72,64,56,58,47,67,63};

    public static void main(String[] args) throws Exception {
        new BeatBox().buildGUI();
    }

    private void buildGUI() throws Exception {
        theFrame = new JFrame("Cyber BeatBox");
        theFrame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        for (UIManager.LookAndFeelInfo lf:UIManager.getInstalledLookAndFeels()) {
            if (lf.getName().equals("GTK+"))
                UIManager.setLookAndFeel(lf.getClassName());
        }
        BorderLayout layout = new BorderLayout();
        JPanel background = new JPanel(layout);
        background.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));

        checkBoxList = new ArrayList<>();
        Box buttonBox = new Box(BoxLayout.Y_AXIS);

        JButton start = new JButton("Начать");
        start.setMaximumSize(new Dimension(200,30));
        start.addActionListener(actionEvent -> buildTrackAndStart());
        buttonBox.add(start);

        JButton stop = new JButton("Остановить");
        stop.setMaximumSize(new Dimension(200,30));
        stop.addActionListener(actionEvent -> sequencer.stop());
        buttonBox.add(stop);

        JButton upTempo = new JButton("Увеличить темп");
        upTempo.setMaximumSize(new Dimension(200,30));
        upTempo.addActionListener(actionEvent -> {
            float tempoFactor = sequencer.getTempoFactor();
            sequencer.setTempoFactor((float) (tempoFactor * 1.03));
        });
        buttonBox.add(upTempo);

        JButton downTempo = new JButton("Замедлить темп");
        downTempo.setMaximumSize(new Dimension(200,30));
        downTempo.addActionListener(actionEvent -> {
            float tempoFactor = sequencer.getTempoFactor();
            sequencer.setTempoFactor((float) (tempoFactor * 0.97));
        });
        buttonBox.add(downTempo);

        JButton serialize = new JButton("Сохранить сэмпл");
        serialize.setMaximumSize(new Dimension(200,30));
        serialize.addActionListener(actionEvent -> saveSample());
        buttonBox.add(serialize);

        JButton readSer = new JButton("Загрузить сэмпл");
        readSer.setMaximumSize(new Dimension(200,30));
        readSer.addActionListener(actionEvent -> loadSample());
        buttonBox.add(readSer);

        Box nameBox = new Box(BoxLayout.Y_AXIS);
        for (int i=0; i < 16; i++) {
            nameBox.add(new Label(instrumentNames[i]));
        }

        background.add(BorderLayout.EAST, buttonBox);
        background.add(BorderLayout.WEST, nameBox);

        theFrame.getContentPane().add(background);

        GridLayout grid = new GridLayout(16,16);
        grid.setVgap(1);
        grid.setHgap(2);
        mainPanel = new JPanel(grid);
        background.add(BorderLayout.CENTER, mainPanel);

        for (int i=0; i < 256; i++) {
            JCheckBox c = new JCheckBox();
            c.setSelected(false);
            checkBoxList.add(c);
            mainPanel.add(c);
        }

        setUpMidi();
        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        theFrame.setBounds(50,50,300,300);
        theFrame.pack();
        theFrame.setVisible(true);
    }

    private void setUpMidi() {
        try {
            sequencer = MidiSystem.getSequencer();
            sequencer.open();
            sequence = new Sequence(Sequence.PPQ,4);
            track = sequence.createTrack();
            sequencer.setTempoInBPM(120);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void buildTrackAndStart() {
        int[] trackList;
        sequence.deleteTrack(track);
        track = sequence.createTrack();
        for (int i=0; i< 16; i++) {
            trackList = new int[16];
            int key = instruments[i];
            for (int j=0; j < 16; j++) {
                JCheckBox jc = checkBoxList.get(j + (16*i));
                if (jc.isSelected())
                    trackList[j] = key;
                else
                    trackList[j] = 0;
            }
            makeTracks(trackList);
            track.add(makeEvent(176,1,127,0,16));
        }
        track.add(makeEvent(192,9,1,0,15));
        try {
            sequencer.setSequence(sequence);
            sequencer.setLoopCount(Sequencer.LOOP_CONTINUOUSLY);
            sequencer.start();
            sequencer.setTempoInBPM(120);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void makeTracks(int[] list) {
        for (int i=0; i < 16; i++) {
            int key = list[i];
            if (key != 0) {
                track.add(makeEvent(144,9,key,100, i));
                track.add(makeEvent(128,9,key,100,i+1));
            }
        }
    }

    private MidiEvent makeEvent(int comd, int chan, int one, int two, int tick) {
        MidiEvent event = null;
        try {
            ShortMessage a = new ShortMessage();
            a.setMessage(comd, chan, one, two);
            event = new MidiEvent(a, tick);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return event;
    }

    private void saveSample() {
        boolean[] checkboxState = new boolean[256];
        for (int i=0; i< 256; i++) {
            JCheckBox check = checkBoxList.get(i);
            if (check.isSelected())
                checkboxState[i] = true;
        }
        try {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.showSaveDialog(theFrame);
            //FileOutputStream fileStream = new FileOutputStream(new File("Checkbox.ser"));
            FileOutputStream fileStream = new FileOutputStream(fileChooser.getSelectedFile());
            ObjectOutputStream os = new ObjectOutputStream(fileStream);
            os.writeObject(checkboxState);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadSample() {
        boolean[] checkboxState = null;
        try {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.showOpenDialog(theFrame);
            FileInputStream fileIn = new FileInputStream(fileChooser.getSelectedFile());
            ObjectInputStream is = new ObjectInputStream(fileIn);
            checkboxState = (boolean[]) is.readObject();
        } catch (Exception e) {
            e.printStackTrace();
        }
        for (int i=0; i< 256; i++) {
            JCheckBox check = checkBoxList.get(i);
            assert checkboxState != null;
            if (checkboxState[i])
                check.setSelected(true);
            else
                check.setSelected(false);
        }
        sequencer.stop();
        buildTrackAndStart();
    }
}