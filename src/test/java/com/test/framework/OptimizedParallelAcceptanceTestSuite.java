package com.test.framework;

import ch.lambdaj.Lambda;
import net.serenitybdd.jbehave.SerenityStories;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

public class OptimizedParallelAcceptanceTestSuite extends SerenityStories {
    private static final Logger LOG = LoggerFactory.getLogger(OptimizedParallelAcceptanceTestSuite.class.getSimpleName());


    private static final String SPLIT_REGEXP = "[, ]{1,2}";

    private static <K, V extends Comparable<? super V>> Map<K, V> sortByValue(final Map<K, V> map) {
        return map.entrySet().stream().sorted((o1, o2) -> o2.getValue().compareTo(o1.getValue()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (v1, v2) -> v1, LinkedHashMap::new));
    }

    void selectStoryFilesForRunningSuite() {
        parallelAcceptanceTestSuite(getSuiteStoryPaths());
    }

    private void parallelAcceptanceTestSuite(final List<String> storyPaths) {
        final int agentPosition = Integer.parseInt(System.getProperty("parallel.agent.number"));
        final int agentCount = Integer.parseInt(System.getProperty("parallel.agent.total"));

        failIfAgentPropertiesAreNotConfiguredCorrectly(agentPosition, agentCount);
        failIfThereAreMoreAgentsThanStories(agentCount, storyPaths.size());

        List<String> resultStoriesForBatch = new ArrayList<>();
        Map<String, Integer> sortedEntryStoryMap = sortByValue(getStoryPathsWithWeight(storyPaths));
        List<Integer> batchWeightsList = new ArrayList<>(Collections.nCopies(agentCount, 0));

        sortedEntryStoryMap.entrySet().stream().forEach(entry -> {
            int minValue = Collections.min(batchWeightsList);
            int minIndex = batchWeightsList.indexOf(minValue);
            batchWeightsList.set(minIndex, minValue + entry.getValue());
            if (agentPosition - 1 == minIndex) {
                resultStoriesForBatch.add(entry.getKey());
            }
        });

        outputWhichStoriesAreBeingRun(resultStoriesForBatch);
        findStoriesCalled(Lambda.join(resultStoriesForBatch, ";"));
    }

    private Map<String, Integer> getStoryPathsWithWeight(final List<String> stories) {
        return stories.stream().collect(Collectors.toMap(storyPath -> storyPath, this::getWeightForStory));
    }

    private int getWeightForStory(final String storyPath) {
        int weight = 0;
        boolean isExamples = false;
        for (String line : getStoryFileLines(storyPath, false)) {
            if (line.startsWith("Scenario:")) {
                weight++;
                isExamples = false;
            } else if (line.startsWith("Examples:")) {
                isExamples = true;
                weight -= 2;
            } else if (isExamples && line.startsWith("|")) {
                weight++;
            }
        }
        return weight;
    }

    private void failIfAgentPropertiesAreNotConfiguredCorrectly(final Integer agentPosition, final Integer agentCount) {
        if (agentPosition == null || agentCount == null) {
            throw new IllegalStateException("The agent properties are not defined!");
        } else if (agentPosition < 1 || agentCount < 1 || agentPosition > agentCount) {
            throw new IllegalStateException(
                    String.format("The agent properties are not configured correctly! agentPosition=%d. agentCount=%d",
                            agentPosition, agentCount));
        }
    }

    private void failIfThereAreMoreAgentsThanStories(final Integer agentCount, final int storyCount) {
        if (storyCount < agentCount) {
            throw new IllegalStateException("There are more agents then there are stories, this agent isn't necessary");
        }
    }

    private void outputWhichStoriesAreBeingRun(final List<String> stories) {
        LOG.info("Running stories: ");
        stories.stream().forEach(LOG::info);
    }

    private List<String> getSuiteStoryPaths() {
        return Optional.of(getStoryList(System.getProperty("metafilter"))).filter(this::isNotEmpty)
                .orElseThrow(() -> new IllegalStateException("There are no story to run by selected tags"));
    }

    private boolean isNotEmpty(final List<String> list) {
        return !list.isEmpty();
    }

    private List<String> getStoryList(final String metaFilter) {
        return Optional.ofNullable(metaFilter).map(this::getStoryPathsByMetaTags).orElseGet(this::storyPaths);
    }

    private List<String> getStoryPathsByMetaTags(final String metaFilter) {
        return storyPaths().stream().filter(storyPath -> isSuitableStoryFile(metaFilter, storyPath))
                .collect(Collectors.toList());
    }

    private boolean isSuitableStoryFile(final String metaFilter, final String storyPath) {
        final Set<String> tags = getFileMetaTags(storyPath, false);
        final Set<String> mainTags = getFileMetaTags(storyPath, true);
        return isApplyByTag(tags, metaFilter) && !isDenyByTag(mainTags, metaFilter);
    }

    private Set<String> getFileMetaTags(final String storyPath, final boolean isOnlyGlobal) {
        final List<String> storyFileLines = getStoryFileLines(storyPath, true);
        final int fromStoryLineIndex = 0;
        final int toStoryLineIndex = isOnlyGlobal
                ? Optional.ofNullable(getLineWithFirstScenario(storyFileLines)).map(storyFileLines::indexOf).orElse(0)
                : storyFileLines.size();
        return storyFileLines.subList(fromStoryLineIndex, toStoryLineIndex).stream().filter(this::byMetaTags)
                .map(tag -> tag.substring(1)).collect(Collectors.toSet());
    }

    private boolean byMetaTags(final String line) {
        return line.startsWith("@") && line.split(" ").length == 1;
    }

    private String getLineWithFirstScenario(final List<String> storyFileLines) {
        return storyFileLines.stream().filter(line -> line.startsWith("Scenario:")).findFirst().orElse(null);
    }

    private List<String> getStoryFileLines(final String storyPath, final boolean isUseCharset) {
        final File file = new File(getClass().getResource("/").getPath(), storyPath);
        List<String> storyFileLines = new ArrayList<>();
        try {
            storyFileLines = isUseCharset ? Files.readAllLines(Paths.get(file.getPath()), Charset.defaultCharset())
                    : Files.readAllLines(Paths.get(file.getPath()));
        } catch (IOException e) {
            LOG.error(String.valueOf(e));
        }
        return storyFileLines;
    }

    private boolean isApplyByTag(final Set<String> fileTags, final String metaFilter) {
        return getMetaStartedWith("+", metaFilter).stream().anyMatch(fileTags::contains);
    }

    private boolean isDenyByTag(final Set<String> fileTags, final String metaFilter) {
        return getMetaStartedWith("-", metaFilter).stream().anyMatch(fileTags::contains);
    }

    private Set<String> getMetaStartedWith(final String symbol, final String metaFilter) {
        return Arrays.stream(metaFilter.split(SPLIT_REGEXP)).filter(tag -> tag.startsWith(symbol))
                .map(tag -> tag.substring(1)).collect(Collectors.toSet());
    }
}
