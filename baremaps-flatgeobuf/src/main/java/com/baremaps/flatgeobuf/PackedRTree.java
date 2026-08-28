/*
 * Licensed under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.baremaps.flatgeobuf;

import com.baremaps.flatgeobuf.generated.Header;
import com.google.common.io.LittleEndianDataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.*;
import org.locationtech.jts.geom.Envelope;

/**
 * This code has been adapted from FlatGeoBuf (BSD 2-Clause "Simplified" License).
 * <p>
 * Copyright (c) 2018, Björn Harrtell
 */
@SuppressWarnings("squid:S117")
public class PackedRTree {

  private static final int HILBERT_MAX = (1 << 16) - 1;
  private static final int NODE_ITEM_LEN = 8 * 4 + 8;
  private static final String ILLEGAL_NODE_SIZE = "Node size must be at least 2";
  private static final String ILLEGAL_NUMBER_OF_ITEMS = "Number of items must be greater than 0";
  private int numItems;
  private int nodeSize;
  private NodeItem[] nodeItems;
  private long numNodes;
  private List<Pair<Integer, Integer>> levelBounds;

  public PackedRTree(final List<Item> items, final short nodeSize) {
    this.numItems = items.size();
    init(nodeSize);
    int k = (int) (this.numNodes - this.numItems);
    Iterator<Item> it = items.iterator();
    for (int i = 0; i < this.numItems; ++i) {
      this.nodeItems[k++] = it.next().nodeItem();
    }
    generateNodes();
  }

  public void init(int nodeSize) {
    if (nodeSize < 2) {
      throw new IllegalArgumentException(ILLEGAL_NODE_SIZE);
    }
    if (numItems == 0) {
      throw new IllegalArgumentException(ILLEGAL_NUMBER_OF_ITEMS);
    }
    this.nodeSize = Math.min(nodeSize, HILBERT_MAX);
    this.levelBounds = generateLevelBounds(numItems, this.nodeSize);
    this.numNodes = levelBounds.get(0).second;
    this.nodeItems = new NodeItem[Math.toIntExact(numNodes)];
  }

  void generateNodes() {
    long pos;
    long end;
    for (int i = 0; i < levelBounds.size() - 1; i++) {
      pos = levelBounds.get(i).first;
      end = levelBounds.get(i).second;
      long newpos = levelBounds.get(i + 1).first;
      while (pos < end) {
        NodeItem node = new NodeItem(pos);
        for (long j = 0; j < this.nodeSize && pos < end; j++) {
          node.expand(nodeItems[(int) pos++]);
        }
        nodeItems[(int) newpos++] = node;
      }
    }
  }

  public static List<Item> hilbertSort(List<Item> items, NodeItem extent) {
    double minX = extent.minX();
    double minY = extent.minY();
    double width = extent.width();
    double height = extent.height();
    items.sort((a, b) -> {
      long ha = hibert(a.nodeItem, HILBERT_MAX, minX, minY, width, height);
      long hb = hibert(b.nodeItem, HILBERT_MAX, minX, minY, width, height);
      long delta = ha - hb;
      if (delta > 0) {
        return 1;
      } else if (delta == 0) {
        return 0;
      } else {
        return -1;
      }
    });
    return items;
  }

  public static long hibert(NodeItem nodeItem, int hilbertMax, double minX, double minY,
      double width, double height) {
    long x = 0;
    long y = 0;
    if (width != 0.0) {
      x = (long) Math.floor(hilbertMax * ((nodeItem.minX() + nodeItem.maxX()) / 2 - minX) / width);
    }
    if (height != 0.0) {
      y = (long) Math.floor(hilbertMax * ((nodeItem.minY() + nodeItem.maxY()) / 2 - minY) / height);
    }
    return hibert(x, y);
  }

  // Based on public domain code at https://github.com/rawrunprotected/hilbert_curves
  private static long hibert(long x, long y) {
    long a = x ^ y;
    long b = 0xFFFF ^ a;
    long c = 0xFFFF ^ (x | y);
    long d = x & (y ^ 0xFFFF);
    long A = a | (b >> 1);
    long B = (a >> 1) ^ a;
    long C = ((c >> 1) ^ (b & (d >> 1))) ^ c;
    long D = ((a & (c >> 1)) ^ (d >> 1)) ^ d;

    a = A;
    b = B;
    c = C;
    d = D;
    A = ((a & (a >> 2)) ^ (b & (b >> 2)));
    B = ((a & (b >> 2)) ^ (b & ((a ^ b) >> 2)));
    C ^= ((a & (c >> 2)) ^ (b & (d >> 2)));
    D ^= ((b & (c >> 2)) ^ ((a ^ b) & (d >> 2)));

    a = A;
    b = B;
    c = C;
    d = D;
    A = ((a & (a >> 4)) ^ (b & (b >> 4)));
    B = ((a & (b >> 4)) ^ (b & ((a ^ b) >> 4)));
    C ^= ((a & (c >> 4)) ^ (b & (d >> 4)));
    D ^= ((b & (c >> 4)) ^ ((a ^ b) & (d >> 4)));

    a = A;
    b = B;
    c = C;
    d = D;
    C ^= ((a & (c >> 8)) ^ (b & (d >> 8)));
    D ^= ((b & (c >> 8)) ^ ((a ^ b) & (d >> 8)));

    a = C ^ (C >> 1);
    b = D ^ (D >> 1);

    long i0 = x ^ y;
    long i1 = b | (0xFFFF ^ (i0 | a));

    i0 = (i0 | (i0 << 8)) & 0x00FF00FF;
    i0 = (i0 | (i0 << 4)) & 0x0F0F0F0F;
    i0 = (i0 | (i0 << 2)) & 0x33333333;
    i0 = (i0 | (i0 << 1)) & 0x55555555;

    i1 = (i1 | (i1 << 8)) & 0x00FF00FF;
    i1 = (i1 | (i1 << 4)) & 0x0F0F0F0F;
    i1 = (i1 | (i1 << 2)) & 0x33333333;
    i1 = (i1 | (i1 << 1)) & 0x55555555;

    return ((i1 << 1) | i0);
  }

  public static NodeItem calcExtent(List<Item> items) {
    return items.stream()
        .map(Item::nodeItem)
        .reduce(new NodeItem(0), NodeItem::expand);
  }

  public void write(OutputStream outputStream) throws IOException {
    // nodeItem 40 Byte
    ByteBuffer buffer = ByteBuffer.allocate((int) (NODE_ITEM_LEN * numNodes));
    buffer.order(ByteOrder.LITTLE_ENDIAN);
    for (NodeItem nodeItem : nodeItems) {
      buffer.putDouble(nodeItem.minX());
      buffer.putDouble(nodeItem.minY());
      buffer.putDouble(nodeItem.maxX());
      buffer.putDouble(nodeItem.maxY());
      buffer.putLong(nodeItem.offset());
    }
    buffer.flip();
    try {
      if (buffer.hasRemaining()) {
        byte[] arr = new byte[buffer.remaining()];
        buffer.get(arr);
        outputStream.write(arr);
        outputStream.flush();
      }
    } finally {
      buffer.clear();
      buffer = null;
    }
  }

  public static long calcSize(long numItems, int nodeSize) {
    if (nodeSize < 2) {
      throw new IllegalArgumentException(ILLEGAL_NODE_SIZE);
    }
    if (numItems == 0) {
      throw new IllegalArgumentException(ILLEGAL_NUMBER_OF_ITEMS);
    }
    int nodeSizeMin = Math.min(nodeSize, 65535);
    // limit so that resulting size in bytes can be represented by ulong
    if (numItems > 1L << 56) {
      throw new IndexOutOfBoundsException("Number of items must be less than 2^56");
    }
    long n = numItems;
    long numNodes = n;
    do {
      n = (n + nodeSizeMin - 1) / nodeSizeMin;
      numNodes += n;
    } while (n != 1);
    return numNodes * NODE_ITEM_LEN;
  }

  static List<Pair<Integer, Integer>> generateLevelBounds(int numItems, int nodeSize) {
    if (nodeSize < 2) {
      throw new IllegalArgumentException(ILLEGAL_NODE_SIZE);
    }
    if (numItems == 0) {
      throw new IllegalArgumentException(ILLEGAL_NUMBER_OF_ITEMS);
    }

    // number of nodes per level in bottom-up order
    int n = numItems;
    int numNodes = n;
    ArrayList<Integer> levelNumNodes = new ArrayList<Integer>();
    levelNumNodes.add(n);
    do {
      n = (n + nodeSize - 1) / nodeSize;
      numNodes += n;
      levelNumNodes.add(n);
    } while (n != 1);

    // offsets per level in reversed storage order (top-down)
    ArrayList<Integer> levelOffsets = new ArrayList<Integer>();
    n = numNodes;
    for (int size : levelNumNodes) {
      n -= size;
      levelOffsets.add(n);
    }
    List<Pair<Integer, Integer>> levelBounds = new LinkedList<>();
    // bounds per level in reversed storage order (top-down)
    for (int i = 0; i < levelNumNodes.size(); i++) {
      levelBounds.add(new Pair<>(levelOffsets.get(i), levelOffsets.get(i) + levelNumNodes.get(i)));
    }
    return levelBounds;
  }

  public record QueueItem(long nodeIndex, int level) {

  }

  public record SearchHit(long offset, long index) {

  }

  @SuppressWarnings("squid:S3776")
  public static List<SearchHit> search(
      ByteBuffer bb,
      int start,
      int numItems,
      int nodeSize,
      Envelope rect) {
    ArrayList<SearchHit> searchHits = new ArrayList<SearchHit>();
    double minX = rect.getMinX();
    double minY = rect.getMinY();
    double maxX = rect.getMaxX();
    double maxY = rect.getMaxY();
    List<Pair<Integer, Integer>> levelBounds = generateLevelBounds(numItems, nodeSize);
    int leafNodesOffset = levelBounds.get(0).first;
    int numNodes = levelBounds.get(0).second;
    Deque<QueueItem> queue = new LinkedList<QueueItem>();
    queue.add(new QueueItem(0, levelBounds.size() - 1));
    while (!queue.isEmpty()) {
      QueueItem stackItem = queue.pop();
      int nodeIndex = (int) stackItem.nodeIndex;
      int level = stackItem.level;
      boolean isLeafNode = nodeIndex >= numNodes - numItems;
      // find the end index of the node
      int levelEnd = levelBounds.get(level).second;
      int end = Math.min(nodeIndex + nodeSize, levelEnd);
      int nodeStart = start + (nodeIndex * NODE_ITEM_LEN);
      // search through child nodes
      for (int pos = nodeIndex; pos < end; pos++) {
        int offset = nodeStart + ((pos - nodeIndex) * NODE_ITEM_LEN);
        double nodeMinX = bb.getDouble(offset + 0);
        double nodeMinY = bb.getDouble(offset + 8);
        double nodeMaxX = bb.getDouble(offset + 16);
        double nodeMaxY = bb.getDouble(offset + 24);
        if (maxX < nodeMinX) {
          continue;
        }
        if (maxY < nodeMinY) {
          continue;
        }
        if (minX > nodeMaxX) {
          continue;
        }
        if (minY > nodeMaxY) {
          continue;
        }
        long indexOffset = bb.getLong(offset + 32);
        if (isLeafNode) {
          searchHits.add(new SearchHit(indexOffset, (long) pos - leafNodesOffset));
        } else {
          queue.add(new QueueItem(indexOffset, level - 1));
        }
      }
    }
    return searchHits;
  }

  public record SearchResult(List<SearchHit> hits, int pos) {

  }

  @SuppressWarnings("squid:S3776")
  public static SearchResult search(
      InputStream stream,
      int start,
      int numItems,
      int nodeSize,
      Envelope rect) throws IOException {
    LittleEndianDataInputStream data = new LittleEndianDataInputStream(stream);
    int dataPos = 0;
    int skip;
    List<SearchHit> hits = new ArrayList<>();
    double minX = rect.getMinX();
    double minY = rect.getMinY();
    double maxX = rect.getMaxX();
    double maxY = rect.getMaxY();
    List<Pair<Integer, Integer>> levelBounds = generateLevelBounds(numItems, nodeSize);
    int leafNodesOffset = levelBounds.get(0).first;
    int numNodes = levelBounds.get(0).second;
    Deque<QueueItem> queue = new LinkedList<QueueItem>();
    queue.add(new QueueItem(0, levelBounds.size() - 1));
    while (!queue.isEmpty()) {
      QueueItem stackItem = queue.pop();
      int nodeIndex = (int) stackItem.nodeIndex;
      int level = stackItem.level;
      boolean isLeafNode = nodeIndex >= numNodes - numItems;
      // find the end index of the node
      int levelBound = levelBounds.get(level).second;
      int end = Math.min(nodeIndex + nodeSize, levelBound);
      int nodeStart = nodeIndex * NODE_ITEM_LEN;
      skip = nodeStart - dataPos;
      if (skip > 0) {
        skipNBytes(data, skip);
        dataPos += skip;
      }
      // search through child nodes
      for (int pos = nodeIndex; pos < end; pos++) {
        int offset = nodeStart + ((pos - nodeIndex) * NODE_ITEM_LEN);
        skip = offset - dataPos;
        if (skip > 0) {
          skipNBytes(data, skip);
          dataPos += skip;
        }
        double nodeMinX = data.readDouble();
        dataPos += 8;
        if (maxX < nodeMinX) {
          continue;
        }
        double nodeMinY = data.readDouble();
        dataPos += 8;
        if (maxY < nodeMinY) {
          continue;
        }
        double nodeMaxX = data.readDouble();
        dataPos += 8;
        if (minX > nodeMaxX) {
          continue;
        }
        double nodeMaxY = data.readDouble();
        dataPos += 8;
        if (minY > nodeMaxY) {
          continue;
        }
        long indexOffset = data.readLong();
        dataPos += 8;
        if (isLeafNode) {
          hits.add(new SearchHit(indexOffset, (long) pos - leafNodesOffset));
        } else {
          queue.add(new QueueItem(indexOffset, level - 1));
        }
      }
    }
    return new SearchResult(hits, dataPos);
  }

  public static long[] readFeatureOffsets(
      LittleEndianDataInputStream data,
      long[] fids,
      Header header) throws IOException {

    long treeSize = calcSize((int) header.featuresCount(), header.indexNodeSize());
    List<Pair<Integer, Integer>> levelBounds =
        generateLevelBounds((int) header.featuresCount(), header.indexNodeSize());
    long bottomLevelOffset = levelBounds.get(0).first * 40L;

    long pos = 0;
    long[] featureOffsets = new long[fids.length];
    for (int i = 0; i < fids.length; i++) {
      if (fids[i] > header.featuresCount() - 1) {
        throw new NoSuchElementException();
      }
      long nodeItemOffset = bottomLevelOffset + (fids[i] * 40);
      long delta = nodeItemOffset + (8 * 4) - pos;
      skipNBytes(data, delta);
      long featureOffset = data.readLong();
      pos += delta + 8;
      featureOffsets[i] = featureOffset;
    }
    long remainingIndexOffset = treeSize - pos;
    skipNBytes(data, remainingIndexOffset);

    return featureOffsets;
  }

  static void skipNBytes(InputStream stream, long skip) throws IOException {
    long remaining = skip;
    while (remaining > 0) {
      remaining -= stream.skip(remaining);
    }
  }

  public record Item(NodeItem nodeItem) {

  }

  public record Pair<T, U> (T first, U second) {

  }

  public record NodeItem(
      double minX,
      double minY,
      double maxX,
      double maxY,
      long offset) {

    public NodeItem(double minX, double minY, double maxX, double maxY) {
      this(minX, minY, maxX, maxY, 0);
    }

    public NodeItem(long offset) {
      this(
          Double.POSITIVE_INFINITY,
          Double.POSITIVE_INFINITY,
          Double.NEGATIVE_INFINITY,
          Double.NEGATIVE_INFINITY,
          offset);
    }

    public double width() {
      return maxX - minX;
    }

    public double height() {
      return maxY - minY;
    }

    public static NodeItem sum(NodeItem a, final NodeItem b) {
      return a.expand(b);
    }

    public NodeItem expand(final NodeItem nodeItem) {
      return new NodeItem(
          Math.min(nodeItem.minX, minX),
          Math.min(nodeItem.minY, minY),
          Math.max(nodeItem.maxX, maxX),
          Math.max(nodeItem.maxY, maxY),
          offset);
    }

    public boolean intersects(NodeItem nodeItem) {
      if (nodeItem.minX > maxX) {
        return false;
      } else if (nodeItem.minY > maxY) {
        return false;
      } else if (nodeItem.maxX < minX) {
        return false;
      } else if (nodeItem.maxY < minY) {
        return false;
      } else {
        return true;
      }
    }

    public Envelope toEnvelope() {
      return new Envelope(minX, maxX, minY, maxY);
    }
  }
}
