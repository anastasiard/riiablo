package com.riiablo.table.annotation;

import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.TypeSpec;
import java.util.Date;
import javax.annotation.Generated;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.apache.commons.lang3.time.FastDateFormat;

abstract class CodeGenerator {
  final Context context;
  final String packageName;

  CodeGenerator(Context context, String packageName) {
    this.context = context;
    this.packageName = packageName;
  }

  abstract String formatName(SchemaElement schemaElement);

  JavaFile generate(SchemaElement schemaElement) {
    TypeSpec.Builder serializerTypeSpec
        =newTypeSpec(schemaElement)
        .addAnnotation(newGenerated(schemaElement.element.getQualifiedName().toString()))
        ;

    return JavaFile
        .builder(packageName, serializerTypeSpec.build())
        .skipJavaLangImports(true)
        .addFileComment(
            "automatically generated by $L, do not modify",
            getClass().getSimpleName())
        .build();
  }

  TypeSpec.Builder newTypeSpec(SchemaElement schemaElement) {
    return TypeSpec.classBuilder(formatName(schemaElement));
  }

  AnnotationSpec newGenerated(String comments) {
    return AnnotationSpec
        .builder(Generated.class)
        .addMember("value", "$S", getClass().getCanonicalName())
        .addMember("date", "$S", dateFormat().format(new Date()))
        .addMember("comments", "$S", comments)
        .build();
  }

  FastDateFormat dateFormat() {
    return DateFormatUtils.ISO_8601_EXTENDED_DATETIME_TIME_ZONE_FORMAT;
  }
}
