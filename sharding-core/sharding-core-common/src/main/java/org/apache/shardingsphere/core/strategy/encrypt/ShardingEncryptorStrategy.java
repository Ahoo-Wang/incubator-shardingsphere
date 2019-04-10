/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.shardingsphere.core.strategy.encrypt;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.collect.Collections2;
import lombok.AccessLevel;
import lombok.Getter;
import org.apache.shardingsphere.api.config.encryptor.EncryptorRuleConfiguration;
import org.apache.shardingsphere.core.rule.ColumnNode;
import org.apache.shardingsphere.core.spi.algorithm.encrypt.ShardingEncryptorServiceLoader;
import org.apache.shardingsphere.spi.encrypt.ShardingEncryptor;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * Sharding encryptor strategy.
 *
 * @author panjuan
 */
@Getter
public final class ShardingEncryptorStrategy {
    
    private final List<ColumnNode> columns;
    
    private final List<ColumnNode> assistedQueryColumns;
    
    @Getter(AccessLevel.PRIVATE)
    private final ShardingEncryptor shardingEncryptor;
    
    public ShardingEncryptorStrategy(final EncryptorRuleConfiguration config) {
        this.columns = createColumnNodes(config.getQualifiedColumns());
        this.assistedQueryColumns = Strings.isNullOrEmpty(config.getAssistedQueryColumns()) ? Collections.<ColumnNode>emptyList() : createColumnNodes(config.getAssistedQueryColumns());
        Preconditions.checkArgument(assistedQueryColumns.isEmpty() || assistedQueryColumns.size() == columns.size(), "The size of `columns` and `assistedQueryColumns` is not same.");
        shardingEncryptor = new ShardingEncryptorServiceLoader().newService(config.getType(), config.getProperties());
        shardingEncryptor.init();
    }
    
    private List<ColumnNode> createColumnNodes(final String columnNodeStr) {
        List<ColumnNode> result = new LinkedList<>();
        for (String each : Splitter.on(",").trimResults().splitToList(columnNodeStr)) {
            result.add(new ColumnNode(each));
        }
        return result;
    }
    
    /**
     * Get sharding encryptor.
     *
     * @param logicTableName logic table name
     * @param columnName column name
     * @return optional of sharding encryptor
     */
    public Optional<ShardingEncryptor> getShardingEncryptor(final String logicTableName, final String columnName) {
        return Collections2.filter(columns, new Predicate<ColumnNode>() {
            
            @Override
            public boolean apply(final ColumnNode input) {
                return input.equals(new ColumnNode(logicTableName, columnName));
            }
        }).isEmpty() ? Optional.<ShardingEncryptor>absent() : Optional.of(shardingEncryptor);
    }
}
