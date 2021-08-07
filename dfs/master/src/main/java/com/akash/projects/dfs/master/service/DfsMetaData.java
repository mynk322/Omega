package com.akash.projects.dfs.master.service;

import com.akash.projects.common.dfs.model.DfsChunk;
import com.akash.projects.common.dfs.model.DfsFile;
import com.akash.projects.common.dfs.model.DfsNode;

import java.util.*;

public class DfsMetaData {

    // <registryHost:registryPort, nodeId>
    private static Map<String, Long> nodeIds;
    // <nodeId, dfsNode>
    private static Map<Long, DfsNode> nodeMap;
    // <fileId, filename>
    private static Map<Long, String> fileNameIdMap;
    // <fileName, dfsFile>
    private static Map<String, DfsFile> fileMap;
    // <chunkId, dfsChunk>
    private static Map<Long, DfsChunk> chunkMap;

    public DfsMetaData() {
        nodeIds = new HashMap<>();
        nodeMap = new HashMap<>();
        fileMap = new HashMap<>();
        fileNameIdMap = new HashMap<>();
        chunkMap = new HashMap<>();
    }

    public void addDfsNode(String registryHost, int registryPort, String serviceName) {
        DfsNode node = new DfsNode(DfsNode.counter.incrementAndGet(), registryHost, registryPort, serviceName);
        String key = registryHost + ":" + registryPort;
        if (!nodeIds.containsKey(key)) {
            nodeIds.put(key, node.getId());
            nodeMap.put(node.getId(), node);
        }
    }

    public static void removeDfsNode(String registryHost, int registryPort, String serviceName) {
        String key = registryHost + ":" + registryPort;
        if (nodeIds.containsKey(key)) {
            Long id = nodeIds.get(key);
            nodeIds.remove(key);
            nodeMap.remove(id);
        }
    }

    public void createFile(String fileName, int replicas) {
        DfsFile dfsFile = new DfsFile(DfsFile.counter.incrementAndGet(), fileName, replicas);
        fileMap.put(fileName, dfsFile);
        fileNameIdMap.put(dfsFile.getId(), fileName);
    }

    public void deleteFile(Long fileId) {
        String fileName = fileNameIdMap.get(fileId);
        List<DfsChunk> chunkList = fileMap.get(fileName).getChunks();
        if (!chunkList.isEmpty()) {
            chunkList.forEach(chunk->chunkMap.remove(chunk.getId()));
        }
        fileNameIdMap.remove(fileId);
        fileMap.remove(fileName);
    }

    public boolean createChunk(long fileId, long offset, int size) {
        String fileName = fileNameIdMap.get(fileId);
        DfsFile dfsFile = fileMap.get(fileName);
        if (Objects.nonNull(dfsFile)) {
            long chunkId = DfsChunk.counter.incrementAndGet();
            DfsChunk dfsChunk = new DfsChunk(chunkId, fileId, offset, size);
            // allocate data nodes for the chunk
            List<DfsNode> nodes = allocateDataNodes(dfsFile.getReplicas());
            if (nodes.isEmpty()) {
                return false;
            }
            dfsChunk.setNodes(nodes);
            dfsFile.getChunks().add(dfsChunk);
            fileMap.put(fileName, dfsFile);
            chunkMap.put(chunkId, dfsChunk);
        }
        return true;
    }

    private List<DfsNode> allocateDataNodes(int replicas) {
        List<DfsNode> nodes = new ArrayList<>(nodeMap.values());
        Collections.shuffle(nodes);
        List<DfsNode> allocatedNodes = new ArrayList<>();
        for (int i = 0; i<Math.min(replicas, nodes.size()); i++) {
            allocatedNodes.add(nodes.get(i));
        }
        return allocatedNodes;
    }

    public DfsFile getFile(String fileName) {
        return fileMap.get(fileName);
    }

    public List<DfsFile> listFiles() {
        List<DfsFile> fileList = new ArrayList<>(fileMap.values());
        return fileList;
    }

    public static Map<Long, DfsNode> getNodeMap() {
        return nodeMap;
    }

}