/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.iotdb.session.pool;

import java.sql.SQLException;
import java.util.List;
import org.apache.iotdb.rpc.IoTDBRPCException;
import org.apache.iotdb.session.Session;
import org.apache.iotdb.session.SessionDataSet;
import org.apache.iotdb.tsfile.read.common.RowRecord;

public class SessionDataSetWrapper {
  SessionDataSet sessionDataSet;
  Session session;
  SessionPool pool;

  public SessionDataSetWrapper(SessionDataSet sessionDataSet,
      Session session, SessionPool pool) {
    this.sessionDataSet = sessionDataSet;
    this.session = session;
    this.pool = pool;
  }

  protected Session getSession() {
    return session;
  }

  public int getBatchSize() {
    return sessionDataSet.getBatchSize();
  }

  public void setBatchSize(int batchSize) {
    sessionDataSet.setBatchSize(batchSize);
  }

  public boolean hasNext() throws SQLException, IoTDBRPCException {
    boolean next = sessionDataSet.hasNext();
    if (!next) {
      pool.closeResultSet(this);
    }
    return next;
  }

  public RowRecord next() throws SQLException, IoTDBRPCException {
    return sessionDataSet.next();
  }

  public List<String> getColumnNames() {
    return sessionDataSet.getColumnNames();
  }
}