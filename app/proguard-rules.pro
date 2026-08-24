# Add project specific ProGuard rules here.

-dontwarn io.netty.**
-dontwarn com.hivemq.client.**
-dontwarn org.apache.log4j.**
-dontwarn org.apache.logging.log4j.**
-dontwarn org.eclipse.jetty.**
-dontwarn org.slf4j.**
-dontwarn javax.annotation.**

-keep class com.hivemq.client.** { *; }
-keep class io.netty.** { *; }

