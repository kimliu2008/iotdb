/*
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file distributed with this work for additional information regarding copyright ownership.  The ASF licenses this file to you under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.  You may obtain a copy of the License at      http://www.apache.org/licenses/LICENSE-2.0  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the License for the specific language governing permissions and limitations under the License.
 */

package org.apache.iotdb.cluster.query.reader;

import java.io.IOException;
import java.util.NoSuchElementException;
import org.apache.iotdb.cluster.server.member.MetaGroupMember;
import org.apache.iotdb.db.exception.StorageEngineException;
import org.apache.iotdb.db.exception.metadata.MetadataException;
import org.apache.iotdb.db.query.context.QueryContext;
import org.apache.iotdb.db.query.reader.IPointReader;
import org.apache.iotdb.db.query.timegenerator.EngineNodeConstructor;
import org.apache.iotdb.db.utils.TimeValuePair;
import org.apache.iotdb.tsfile.file.metadata.enums.TSDataType;
import org.apache.iotdb.tsfile.read.common.BatchData;
import org.apache.iotdb.tsfile.read.common.Path;
import org.apache.iotdb.tsfile.read.filter.basic.Filter;
import org.apache.iotdb.tsfile.read.reader.IBatchReader;
import org.apache.iotdb.tsfile.utils.TsPrimitiveType;

public class ClusterNodeConstructor extends EngineNodeConstructor {

  private MetaGroupMember metaGroupMember;

  ClusterNodeConstructor(MetaGroupMember metaGroupMember) {
    this.metaGroupMember = metaGroupMember;
  }

  @Override
  protected IPointReader getSeriesReader(Path path, TSDataType dataType, Filter filter,
      QueryContext context)
      throws IOException, StorageEngineException {
    IBatchReader batchReader = metaGroupMember.getSeriesReader(path, dataType, filter, context, false, true);
    return new BatchedPointReader(batchReader);
  }

  @Override
  protected TSDataType getSeriesType(String path) throws MetadataException {
    return metaGroupMember.getSeriesType(path);
  }

  class BatchedPointReader implements IPointReader {

    private IBatchReader innerReader;
    private BatchData cachedBatch;
    private TimeValuePair cachedPair;

    BatchedPointReader(IBatchReader innerReader) {
      this.innerReader = innerReader;
    }

    @Override
    public boolean hasNext() throws IOException {
      if (cachedPair != null) {
        return true;
      }
      fetch();
      return cachedPair != null;
    }

    private void fetch() throws IOException {
      if (!(cachedBatch == null || !cachedBatch.hasCurrent()) && innerReader.hasNextBatch()) {
        cachedBatch = innerReader.nextBatch();
      }
      if (cachedBatch != null && cachedBatch.hasCurrent()) {
        cachedPair = new TimeValuePair(cachedBatch.currentTime(),
            TsPrimitiveType.getByType(cachedBatch.getDataType(), cachedBatch.currentValue()));
        cachedBatch.next();
      }
    }

    @Override
    public TimeValuePair next() throws IOException {
      if (!hasNext()) {
        throw new NoSuchElementException();
      }
      TimeValuePair ret = cachedPair;
      cachedPair = null;
      return ret;
    }

    @Override
    public TimeValuePair current() throws IOException {
      if (!hasNext()) {
        throw new NoSuchElementException();
      }
      return cachedPair;
    }

    @Override
    public void close() throws IOException {
      innerReader.close();
    }
  }
}