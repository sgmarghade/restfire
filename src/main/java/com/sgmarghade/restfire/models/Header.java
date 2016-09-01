package com.sgmarghade.restfire.models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Created by swapnil on 10/03/16.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Header {
    private String key;
    private String value;
}
