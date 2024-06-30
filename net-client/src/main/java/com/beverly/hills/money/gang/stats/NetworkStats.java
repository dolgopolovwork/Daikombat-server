package com.beverly.hills.money.gang.stats;

import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.distribution.HistogramSnapshot;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import lombok.Getter;

public class NetworkStats implements NetworkStatsReader {

  private static final AtomicInteger COUNTER = new AtomicInteger();
  private static final MeterRegistry METER_REGISTRY = new SimpleMeterRegistry();

  @Getter
  private final DistributionSummary pingDistributionSummary;

  @Getter
  private final DistributionSummary latencyDistributionSummary;

  @Getter
  private final DistributionSummary outboundDistributionSummary;

  @Getter
  private final DistributionSummary inboundDistributionSummary;

  public NetworkStats() {
    this.pingDistributionSummary = DistributionSummary
        .builder("ping.distribution.summary" + COUNTER.incrementAndGet())
        .publishPercentiles(0.5, 0.75, 0.99)
        .register(METER_REGISTRY);
    this.latencyDistributionSummary = DistributionSummary
        .builder("latency.distribution.summary" + COUNTER.incrementAndGet())
        .publishPercentiles(0.5, 0.75, 0.99)
        .register(METER_REGISTRY);
    this.outboundDistributionSummary = DistributionSummary
        .builder("outbound.distribution.summary" + COUNTER.incrementAndGet())
        .publishPercentiles(0.5, 0.75, 0.99)
        .register(METER_REGISTRY);
    this.inboundDistributionSummary = DistributionSummary
        .builder("inbound.distribution.summary" + COUNTER.incrementAndGet())
        .publishPercentiles(0.5, 0.75, 0.99)
        .register(METER_REGISTRY);
  }

  private final AtomicInteger receivedMessages = new AtomicInteger();

  private final AtomicInteger sentMessages = new AtomicInteger();

  private final AtomicLong outboundPayloadBytes = new AtomicLong();

  private final AtomicLong inboundPayloadBytes = new AtomicLong();

  private final AtomicInteger pingMls = new AtomicInteger(-1);

  private final AtomicInteger latencyMls = new AtomicInteger(-1);

  public void setPingMls(int mls) {
    pingMls.set(mls);
    pingDistributionSummary.record(mls);
  }

  public void setLatencyMls(int mls) {
    latencyMls.set(mls);
    latencyDistributionSummary.record(mls);
  }

  public void incReceivedMessages() {
    receivedMessages.incrementAndGet();
  }

  public void incSentMessages() {
    sentMessages.incrementAndGet();
  }


  public void addOutboundPayloadBytes(long bytes) {
    outboundPayloadBytes.addAndGet(bytes);
    outboundDistributionSummary.record(bytes);
  }


  public void addInboundPayloadBytes(long bytes) {
    inboundPayloadBytes.addAndGet(bytes);
    inboundDistributionSummary.record(bytes);
  }


  @Override
  public int getReceivedMessages() {
    return receivedMessages.get();
  }

  @Override
  public int getSentMessages() {
    return sentMessages.get();
  }

  @Override
  public long getOutboundPayloadBytes() {
    return outboundPayloadBytes.get();
  }

  @Override
  public long getInboundPayloadBytes() {
    return inboundPayloadBytes.get();
  }

  @Override
  public int getPingMls() {
    return pingMls.get();
  }

  @Override
  public int getLatencyMls() {
    return latencyMls.get();
  }

  @Override
  public String toString() {
    var pingSnapshot = pingDistributionSummary.takeSnapshot();
    var latencySnapshot = latencyDistributionSummary.takeSnapshot();
    var inboundSnapshot = inboundDistributionSummary.takeSnapshot();
    var outboundSnapshot = outboundDistributionSummary.takeSnapshot();
    return "\npingDistributionSummary: " + distributionSummaryToString(pingSnapshot) +
        "\nlatencyDistributionSummary: " + distributionSummaryToString(latencySnapshot) +
        "\nreceivedMessages:" + receivedMessages +
        "\nsentMessages:" + sentMessages +
        "\noutboundPayloadBytes:" + outboundPayloadBytes +
        "\noutboundDistributionSummary: " + distributionSummaryToString(outboundSnapshot) +
        "\ninboundPayloadBytes:" + inboundPayloadBytes +
        "\ninboundDistributionSummary: probes " + distributionSummaryToString(inboundSnapshot);
  }

  private String distributionSummaryToString(HistogramSnapshot histogramSnapshot) {
    return "probes " + histogramSnapshot.count() + ", " + Arrays.toString(
        histogramSnapshot.percentileValues());
  }

}
