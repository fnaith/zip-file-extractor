package com.ongakucraft.app;

import org.scijava.swing.checkboxtree.CheckBoxNodeData;
import org.scijava.swing.checkboxtree.CheckBoxNodeEditor;
import org.scijava.swing.checkboxtree.CheckBoxNodeRenderer;

import javax.swing.*;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import java.awt.*;
import java.nio.charset.Charset;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.*;
import java.util.zip.ZipFile;

public final class FileTree extends JPanel {
    private final Map<String, List<String>> childrenMap = new HashMap<>();
    private final Map<String, Boolean> checkedMap = new HashMap<>();

    public FileTree(String zipFilePath, Charset charset) {
        initFileStructure(zipFilePath, charset);
        setLayout(new BorderLayout());

        final var treeModel = new DefaultTreeModel(addNodes(null, "/"));
        final var tree = new JTree(treeModel);

        final var renderer = new CheckBoxNodeRenderer();
        tree.setCellRenderer(renderer);

        final var editor = new CheckBoxNodeEditor(tree);
        tree.setCellEditor(editor);
        tree.setEditable(true);

        treeModel.addTreeModelListener(new TreeModelListener() {
            @Override
            public void treeNodesChanged(final TreeModelEvent e) {
                final var sb = new StringBuilder();
                for (final var path : e.getPath()) {
                    final var node = (DefaultMutableTreeNode) path;
                    final var data = (CheckBoxNodeData) node.getUserObject();
                    sb.append(data.getText()).append('/');
                }
                final var dirPath = sb.toString();
                if (null != e.getChildren()) {
                    for (final var chile : e.getChildren()) {
                        final var node = (DefaultMutableTreeNode) chile;
                        final var data = (CheckBoxNodeData) node.getUserObject();
                        final var path = dirPath + data.getText();
                        if (checkedMap.containsKey(path)) {
                            checkedMap.put(path, data.isChecked());
                        } else {
                            checkedMap.put(path + '/', data.isChecked());
                        }
                    }
                } else {
                    final var node = (DefaultMutableTreeNode) e.getPath()[0];
                    final var data = (CheckBoxNodeData) node.getUserObject();
                    checkedMap.put(dirPath, data.isChecked());
                }
            }

            @Override
            public void treeNodesInserted(final TreeModelEvent e) {}

            @Override
            public void treeNodesRemoved(final TreeModelEvent e) {}

            @Override
            public void treeStructureChanged(final TreeModelEvent e) {}
        });

        final var scrollPane = new JScrollPane();
        scrollPane.getViewport().add(tree);
        add(BorderLayout.CENTER, scrollPane);
    }

    public void export(String inputZipFilePath, String outputZipFilePath, Charset charset) {
        final Map<String, String> env = new HashMap<>();
        env.put("create", "true");
        env.put("encoding", charset.name());
        try (final var input = FileSystems.newFileSystem(Paths.get(inputZipFilePath), env)) {
            try (final var output = FileSystems.newFileSystem(Paths.get(outputZipFilePath), env)) {
                for (final var filePath : getExportedFilePathList()) {
                    final var file = input.getPath(filePath);
                    final var directory = output.getPath(file.getParent().toString());
                    Files.createDirectories(directory);
                    Files.write(output.getPath(filePath), Files.readAllBytes(file), StandardOpenOption.CREATE);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public boolean hasCheckedFilePath() {
        return checkedMap.values().stream().anyMatch(Boolean::valueOf);
    }

    private void initFileStructure(String zipFilePath, Charset charset) {
        childrenMap.put("/", new ArrayList<>());
        try (final var zipFile = new ZipFile(zipFilePath, charset)) {
            final var zipEntries = zipFile.entries();
            while (zipEntries.hasMoreElements()) {
                final var filePath = '/' + zipEntries.nextElement().getName();
                final String normalizedFilePath;
                if (filePath.endsWith("/")) {
                    normalizedFilePath = filePath.substring(0, filePath.length() - 1);
                    childrenMap.put(filePath, new ArrayList<>());
                } else {
                    normalizedFilePath = filePath;
                }
                final var pos = normalizedFilePath.lastIndexOf('/');
                final var parent = normalizedFilePath.substring(0, pos + 1);
                childrenMap.get(parent).add(filePath);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private DefaultMutableTreeNode addCheckBoxNode(DefaultMutableTreeNode parent, String path) {
        final var data = new CheckBoxNodeData(getName(path), false);
        final var node = new DefaultMutableTreeNode(data);
        checkedMap.put(path, false);
        if (null != parent) {
            parent.add(node);
        }
        return node;
    }

    private DefaultMutableTreeNode addNodes(DefaultMutableTreeNode parent, String dir) {
        final var node = addCheckBoxNode(parent, dir);
        final List<String> subFolders = new ArrayList<>();
        final List<String> subFiles = new ArrayList<>();
        for (final var child : childrenMap.get(dir)) {
            final var list = isFolder(child) ? subFolders : subFiles;
            list.add(child);
        }
        for (final var subFolder : subFolders) {
            addNodes(node, subFolder);
        }
        for (final var subFile : subFiles) {
            addCheckBoxNode(node, subFile);
        }
        return node;
    }

    private List<String> getExportedFilePathList() {
        final List<String> filePathList = new ArrayList<>();
        final Set<String> visited = new HashSet<>();
        final Deque<String> queue = new ArrayDeque<>();
        for (final var entry : checkedMap.entrySet()) {
            if (entry.getValue()) {
                queue.add(entry.getKey());
            }
        }
        while (!queue.isEmpty()) {
            final var filePath = queue.poll();
            if (!visited.add(filePath)) {
                continue;
            }
            if (isFolder(filePath)) {
                queue.addAll(childrenMap.get(filePath));
            } else {
                filePathList.add(filePath);
            }
        }
        return filePathList;
    }

    private boolean isFolder(String filePath) {
        return childrenMap.containsKey(filePath);
    }

    private static String getName(String path) {
        if (path.length() <= 1) {
            return "";
        }
        final var tokens = path.split("/");
        return tokens[tokens.length - 1];
    }
}
