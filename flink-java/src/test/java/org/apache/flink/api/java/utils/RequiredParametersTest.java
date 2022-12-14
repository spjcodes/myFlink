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
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.flink.api.java.utils;

import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.fail;

/** Tests for RequiredParameter class and its interactions with ParameterTool. */
@Deprecated
class RequiredParametersTest {

    @Test
    void testAddWithAlreadyExistingParameter() throws RequiredParametersException {

        RequiredParameters required = new RequiredParameters();
        required.add(new Option("berlin"));
        assertThatThrownBy(() -> required.add(new Option("berlin")))
                .isInstanceOf(RequiredParametersException.class)
                .hasMessageContaining("Option with key berlin already exists.");
    }

    @Test
    void testStringBasedAddWithAlreadyExistingParameter() throws RequiredParametersException {

        RequiredParameters required = new RequiredParameters();
        required.add("berlin");
        assertThatThrownBy(() -> required.add("berlin"))
                .isInstanceOf(RequiredParametersException.class)
                .hasMessageContaining("Option with key berlin already exists.");
    }

    @Test
    void testApplyToWithMissingParameters() throws RequiredParametersException {

        ParameterTool parameter = ParameterTool.fromArgs(new String[] {});
        RequiredParameters required = new RequiredParameters();
        required.add(new Option("munich"));

        assertThatThrownBy(() -> required.applyTo(parameter))
                .isInstanceOf(RequiredParametersException.class)
                .hasMessageContaining("Missing arguments for:", "munich ");
    }

    @Test
    void testApplyToWithMissingDefaultValues() throws RequiredParametersException {

        ParameterTool parameter = ParameterTool.fromArgs(new String[] {"--berlin"});
        RequiredParameters required = new RequiredParameters();
        required.add(new Option("berlin"));

        assertThatThrownBy(() -> required.applyTo(parameter))
                .isInstanceOf(RequiredParametersException.class)
                .hasMessageContaining("No default value for undefined parameter berlin");
    }

    @Test
    void testApplyToWithInvalidParameterValueBasedOnOptionChoices()
            throws RequiredParametersException {

        ParameterTool parameter = ParameterTool.fromArgs(new String[] {"--berlin", "river"});
        RequiredParameters required = new RequiredParameters();
        required.add(new Option("berlin").choices("city", "metropolis"));

        assertThatThrownBy(() -> required.applyTo(parameter))
                .isInstanceOf(RequiredParametersException.class)
                .hasMessageContaining(
                        "Value river is not in the list of valid choices for key berlin");
    }

    @Test
    void testApplyToWithParameterDefinedOnShortAndLongName() throws RequiredParametersException {

        ParameterTool parameter =
                ParameterTool.fromArgs(new String[] {"--berlin", "value", "--b", "another"});
        RequiredParameters required = new RequiredParameters();
        required.add(new Option("berlin").alt("b"));

        assertThatThrownBy(() -> required.applyTo(parameter))
                .isInstanceOf(RequiredParametersException.class)
                .hasMessageContaining(
                        "Value passed for parameter berlin is ambiguous. "
                                + "Value passed for short and long name.");
    }

    @Test
    void testApplyToMovesValuePassedOnShortNameToLongNameIfLongNameIsUndefined() {
        ParameterTool parameter = ParameterTool.fromArgs(new String[] {"--b", "value"});
        RequiredParameters required = new RequiredParameters();

        try {
            required.add(new Option("berlin").alt("b"));
            parameter = required.applyTo(parameter);
            assertThat(parameter.data).containsEntry("berlin", "value").containsEntry("b", "value");
        } catch (RequiredParametersException e) {
            fail("Exception thrown " + e.getMessage());
        }
    }

    @Test
    void testDefaultValueDoesNotOverrideValuePassedOnShortKeyIfLongKeyIsNotPassedButPresent() {
        ParameterTool parameter = ParameterTool.fromArgs(new String[] {"--berlin", "--b", "value"});
        RequiredParameters required = new RequiredParameters();

        try {
            required.add(new Option("berlin").alt("b").defaultValue("something"));
            parameter = required.applyTo(parameter);
            assertThat(parameter.data).containsEntry("berlin", "value").containsEntry("b", "value");
        } catch (RequiredParametersException e) {
            fail("Exception thrown " + e.getMessage());
        }
    }

    @Test
    void testApplyToWithNonCastableType() throws RequiredParametersException {

        ParameterTool parameter = ParameterTool.fromArgs(new String[] {"--flag", "15"});
        RequiredParameters required = new RequiredParameters();
        required.add(new Option("flag").type(OptionType.BOOLEAN));

        assertThatThrownBy(() -> required.applyTo(parameter))
                .isInstanceOf(RequiredParametersException.class)
                .hasMessageContaining("Value for parameter flag cannot be cast to type BOOLEAN");
    }

    @Test
    void testApplyToWithSimpleOption() {
        ParameterTool parameter = ParameterTool.fromArgs(new String[] {"--berlin", "value"});
        RequiredParameters required = new RequiredParameters();
        try {
            required.add(new Option("berlin"));
            parameter = required.applyTo(parameter);
            assertThat(parameter.data).containsEntry("berlin", "value");
        } catch (RequiredParametersException e) {
            fail("Exception thrown " + e.getMessage());
        }
    }

    @Test
    void testApplyToWithOptionAndDefaultValue() {
        ParameterTool parameter = ParameterTool.fromArgs(new String[] {"--berlin"});
        RequiredParameters required = new RequiredParameters();
        try {
            required.add(new Option("berlin").defaultValue("value"));
            parameter = required.applyTo(parameter);
            assertThat(parameter.data).containsEntry("berlin", "value");
        } catch (RequiredParametersException e) {
            fail("Exception thrown " + e.getMessage());
        }
    }

    @Test
    void testApplyToWithOptionWithLongAndShortNameAndDefaultValue() {
        ParameterTool parameter = ParameterTool.fromArgs(new String[] {"--berlin"});
        RequiredParameters required = new RequiredParameters();
        try {
            required.add(new Option("berlin").alt("b").defaultValue("value"));
            parameter = required.applyTo(parameter);
            assertThat(parameter.data).containsEntry("berlin", "value").containsEntry("b", "value");
        } catch (RequiredParametersException e) {
            fail("Exception thrown " + e.getMessage());
        }
    }

    @Test
    void testApplyToWithOptionMultipleOptionsAndOneDefaultValue() {
        ParameterTool parameter = ParameterTool.fromArgs(new String[] {"--input", "abc"});
        RequiredParameters rq = new RequiredParameters();
        try {
            rq.add("input");
            rq.add(new Option("parallelism").alt("p").defaultValue("1").type(OptionType.INTEGER));
            parameter = rq.applyTo(parameter);
            assertThat(parameter.data)
                    .containsEntry("parallelism", "1")
                    .containsEntry("p", "1")
                    .containsEntry("input", "abc");
        } catch (RequiredParametersException e) {
            fail("Exception thrown " + e.getMessage());
        }
    }

    @Test
    void testApplyToWithMultipleTypes() {
        ParameterTool parameter = ParameterTool.fromArgs(new String[] {});
        RequiredParameters required = new RequiredParameters();
        try {
            required.add(new Option("berlin").defaultValue("value"));
            required.add(new Option("count").defaultValue("15"));
            required.add(new Option("someFlag").alt("sf").defaultValue("true"));

            parameter = required.applyTo(parameter);

            assertThat(parameter.data)
                    .containsEntry("berlin", "value")
                    .containsEntry("count", "15")
                    .containsEntry("someFlag", "true")
                    .containsEntry("sf", "true");

        } catch (RequiredParametersException e) {
            fail("Exception thrown " + e.getMessage());
        }
    }

    @Test
    void testPrintHelpForFullySetOption() {
        RequiredParameters required = new RequiredParameters();
        try {
            required.add(
                    new Option("option")
                            .defaultValue("some")
                            .help("help")
                            .alt("o")
                            .choices("some", "options"));

            String helpText = required.getHelp();
            assertThat(helpText)
                    .contains(
                            "Required Parameters:",
                            "-o, --option",
                            "default: some",
                            "choices: ",
                            "some",
                            "options");

        } catch (RequiredParametersException e) {
            fail("Exception thrown " + e.getMessage());
        }
    }

    @Test
    void testPrintHelpForMultipleParams() {
        RequiredParameters required = new RequiredParameters();
        try {
            required.add("input");
            required.add("output");
            required.add(
                    new Option("parallelism")
                            .alt("p")
                            .help("Set the parallelism for all operators")
                            .type(OptionType.INTEGER));

            String helpText = required.getHelp();
            assertThat(helpText)
                    .contains(
                            "Required Parameters:",
                            "--input",
                            "--output",
                            "-p, --parallelism",
                            "Set the parallelism for all operators")
                    .doesNotContain("choices", "default");
        } catch (RequiredParametersException e) {
            fail("Exception thrown " + e.getMessage());
        }
    }

    @Test
    void testPrintHelpWithMissingParams() {
        RequiredParameters required = new RequiredParameters();

        String helpText = required.getHelp(Arrays.asList("param1", "param2", "paramN"));
        assertThat(helpText).contains("Missing arguments for:", "param1 ", "param2 ", "paramN ");
    }
}
