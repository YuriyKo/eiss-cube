import React from 'react';
import PropTypes from 'prop-types';
import get from 'lodash/get';
import { Labeled } from 'react-admin';
import Typography from '@material-ui/core/Typography';

const CycleField = ({ className, label, source, record = {}, suffix }) => {
    const value = get(record, source);
    return (
        <Labeled label={label}>
            <Typography className={className}
                component='span'
                variant='body2'
            >
                {value && typeof value !== 'string' ? JSON.stringify(value) : value} {suffix}
            </Typography>
        </Labeled>
    );
};

CycleField.propTypes = {
    addLabel: PropTypes.bool,
    label: PropTypes.string,
    source: PropTypes.string.isRequired,
    record: PropTypes.object,
    suffix: PropTypes.string,
    className: PropTypes.string
};

CycleField.displayName = 'CycleField';

CycleField.defaultProps = {
    addLabel: true,
};

export default CycleField;
