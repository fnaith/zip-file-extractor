package com.ongakucraft.app;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.nio.charset.StandardCharsets;

import static javax.swing.WindowConstants.EXIT_ON_CLOSE;

public class ZipFileExtractor {
    private static FileTree fileTree = null;
    private static JButton exportButton = null;

    public static void main(String[] args) {
        final var charset = StandardCharsets.ISO_8859_1;

        final var frame = new JFrame("FileTree");
        frame.setForeground(Color.black);
        frame.setBackground(Color.lightGray);

        final var cp = frame.getContentPane();

        final var selectButton = new JButton("Select A Zip File");
        selectButton.addActionListener(selectEvent -> {
            final var inputFileChooser = buildJFileChooser();
            if (JFileChooser.APPROVE_OPTION == inputFileChooser.showOpenDialog(null)) {
                if (null != fileTree) {
                    cp.remove(fileTree);
                    cp.remove(exportButton);
                    frame.pack();
                }

                final var inputZipFilePath = inputFileChooser.getSelectedFile().getPath();
                try {
                    fileTree = new FileTree(inputZipFilePath, charset);
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(frame, e.getMessage());
                }
                cp.add(fileTree);

                exportButton = new JButton("Extract Selected Files");
                exportButton.addActionListener(exportEvent -> {
                    if (fileTree.getExportedFilePathList().isEmpty()) {
                        JOptionPane.showMessageDialog(frame, "No file selected !");
                        return;
                    }
                    final var outputFileChooser = buildJFileChooser();
                    if (JFileChooser.APPROVE_OPTION == outputFileChooser.showOpenDialog(null)) {
                        final var outputZipFilePath = outputFileChooser.getSelectedFile().getPath();
                        try {
                            fileTree.export(inputZipFilePath, outputZipFilePath, charset);
                            JOptionPane.showMessageDialog(frame, "Done !");
                        } catch (Exception e) {
                            JOptionPane.showMessageDialog(frame, e.getMessage());
                        }
                    }
                });
                cp.add(exportButton, BorderLayout.SOUTH);
                frame.pack();
            }
        });
        cp.add(selectButton, BorderLayout.NORTH);

        frame.pack();
        frame.setVisible(true);
        frame.setDefaultCloseOperation(EXIT_ON_CLOSE);
    }

    private static JFileChooser buildJFileChooser() {
        final var fileChooser = new JFileChooser();
        final var zipFilter = new FileNameExtensionFilter("Zip files (*.zip,*.jar)", "zip", "jar");
        fileChooser.setFileFilter(zipFilter);
        fileChooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
        return fileChooser;
    }
}
