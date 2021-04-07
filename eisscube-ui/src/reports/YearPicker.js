import React, { useState, useEffect } from "react";
import { DatePicker } from "@material-ui/pickers";
import { withStyles } from "@material-ui/core";

const styles = theme => ({
	date: {
		marginTop: theme.spacing(1)-2,
		width: theme.spacing(16)
	}
});

function YearPicker({classes, date, onChange}) {
	const [value, setValue] = useState(date);

	useEffect(() => {
		setValue(date);
	}, [date]);

	const changeDate = (new_date) => {
		setValue(new_date);
		onChange(new_date);
	};

	return (
		<DatePicker className={classes.date}
			autoOk
			variant='inline'
			views={['year']}
			label={false}
			disableFuture={true}
			value={value}
			onChange={changeDate}
		/>
	);
}

export default withStyles(styles)(YearPicker);
