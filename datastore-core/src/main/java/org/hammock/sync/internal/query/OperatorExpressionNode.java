/*
 * Copyright © 2017 IBM Corp. All rights reserved.
 *
 * Copyright © 2015 Cloudant, Inc. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions
 * and limitations under the License.
 */

package org.hammock.sync.internal.query;

import java.util.Map;

/**
 *  An OperatorExpressionNode object is used by the
 *  {@link UnindexedMatcher}.  Since some methods within the
 *  {@link UnindexedMatcher} class that utilize an OperatorExpressionNode
 *  object are static in nature, the OperatorExpressionNode class cannot be implemented as an
 *  inner class to {@link UnindexedMatcher} and must be
 *  implemented this way instead.
 *
 *  @see UnindexedMatcher
 */
class OperatorExpressionNode implements QueryNode {

    final Map<String, Object> expression;

    OperatorExpressionNode(Map<String, Object> expression){
        this.expression = expression;
    }

}
