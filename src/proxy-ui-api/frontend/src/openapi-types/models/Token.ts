/*
 * The MIT License
 * Copyright (c) 2019- Nordic Institute for Interoperability Solutions (NIIS)
 * Copyright (c) 2018 Estonian Information System Authority (RIA),
 * Nordic Institute for Interoperability Solutions (NIIS), Population Register Centre (VRK)
 * Copyright (c) 2015-2017 Estonian Information System Authority (RIA), Population Register Centre (VRK)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
import type { Key } from './Key';
import type { KeyValuePair } from './KeyValuePair';
import type { PossibleActions } from './PossibleActions';
import type { TokenStatus } from './TokenStatus';
import type { TokenType } from './TokenType';

/**
 * Token. Also includes the possible actions that can be done to this object, e.g DELETE (only for token related operations and does not consider user authorization).
 */
export interface Token {
    /**
     * token id
     */
    readonly id: string;
    /**
     * token name
     */
    name: string;
    type: TokenType;
    /**
     * token keys
     */
    keys: Array<Key>;
    status: TokenStatus;
    /**
     * if the token has been logged in to
     */
    logged_in: boolean;
    /**
     * if the token is available
     */
    available: boolean;
    /**
     * if the token is saved to configuration
     */
    saved_to_configuration: boolean;
    /**
     * if the token is read-only
     */
    read_only: boolean;
    /**
     * serial number of the token
     */
    serial_number?: string;
    /**
     * Contains label-value pairs of information
     */
    token_infos?: Array<KeyValuePair>;
    possible_actions?: PossibleActions;
}
