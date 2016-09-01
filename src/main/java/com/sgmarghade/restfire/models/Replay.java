package com.sgmarghade.restfire.models;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Created by swapnil on 07/06/16.
 */
@NoArgsConstructor
@AllArgsConstructor
@Data
public class Replay {
    private Long fromTime;
    private Long toTime;
    private List<String> ids;
}
