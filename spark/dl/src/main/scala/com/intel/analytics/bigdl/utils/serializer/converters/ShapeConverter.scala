/*
 * Copyright 2016 The BigDL Authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.intel.analytics.bigdl.utils.serializer.converters

import com.intel.analytics.bigdl.tensor.TensorNumericMath
import com.intel.analytics.bigdl.utils.serializer.{DeserializeContext, SerializeContext}
import com.intel.analytics.bigdl.utils.{MultiShape, SingleShape, Shape => BigDLShape}
import serialization.Bigdl
import serialization.Bigdl.Shape.ShapeType
import serialization.Bigdl.{AttrValue, DataType, Shape}

import scala.collection.JavaConverters._
import scala.reflect.ClassTag
import scala.reflect.runtime.universe

object ShapeConverter extends DataConverter {
  override def getAttributeValue[T: ClassTag]
  (context: DeserializeContext, attribute: Bigdl.AttrValue)
  (implicit ev: TensorNumericMath.TensorNumeric[T]): AnyRef = {
    val shape = attribute.getShape
    toBigDLShape(shape)
  }

  private def toBigDLShape(shape : Shape): BigDLShape = {
    if (shape.getShapeType == ShapeType.SINGLE) {
      val shapeValues = shape.getShapeValueList.asScala.toList.map(_.intValue)
      SingleShape(shapeValues)
    } else if (shape.getShapeType == ShapeType.MULTI) {
      val shapes = shape.getShapeList.asScala.toList.map(toBigDLShape(_))
      MultiShape(shapes)
    } else {
      throw new RuntimeException(s"${shape.getShapeType} not supported for now")
    }
  }

  override def setAttributeValue[T: ClassTag]
  (context: SerializeContext[T], attributeBuilder: AttrValue.Builder,
   value: Any, valueType: universe.Type)(implicit ev: TensorNumericMath.TensorNumeric[T]): Unit = {
    attributeBuilder.setDataType(DataType.SHAPE)
    if (value != null) {
      val shape = value.asInstanceOf[BigDLShape]
      val shapeBuilder = Shape.newBuilder
      setShape(shape, shapeBuilder)
      attributeBuilder.setShape(shapeBuilder.build)
    }
  }

  private def setShape(bigdlShape : BigDLShape, shapeBuilder : Shape.Builder): Unit = {
    if (bigdlShape.isInstanceOf[SingleShape]) {
      shapeBuilder.setShapeType(ShapeType.SINGLE)
      val shapes = bigdlShape.toSingle
      shapeBuilder.setSsize(shapes.size)
      shapes.foreach(shape => {
        shapeBuilder.addShapeValue(shape)
      })
    } else if (bigdlShape.isInstanceOf[MultiShape]) {
      shapeBuilder.setShapeType(ShapeType.MULTI)
      val shapes = bigdlShape.toMulti
      shapeBuilder.setSsize(shapes.size)
      shapes.foreach(shape => {
        val subShapeBuilder = Shape.newBuilder
        setShape(shape, subShapeBuilder)
        shapeBuilder.addShape(subShapeBuilder.build)
      })
    } else {
      throw new RuntimeException(s"${bigdlShape} type not supported !")
    }
  }
}
